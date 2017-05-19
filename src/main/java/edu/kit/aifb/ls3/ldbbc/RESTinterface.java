package edu.kit.aifb.ls3.ldbbc;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.semanticweb.yars.jaxrs.header.AcceptPostNxPrdfSerialisations;
import org.semanticweb.yars.jaxrs.header.HeaderField;
import org.semanticweb.yars.jaxrs.header.InjectHeaders;
import org.semanticweb.yars.jaxrs.trailingslash.NotFoundOnTrailingSlash;
import org.semanticweb.yars.jaxrs.trailingslash.RedirectMissingTrailingSlash;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.NodesReResolvingIterator;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.LDP;
import org.semanticweb.yars.nx.namespace.RDF;

import edu.kit.aifb.ls3.ldbbc.DerServletContextListener.ServletContextAttributes;

@Path("/")
@RedirectMissingTrailingSlash
@InjectHeaders({ @HeaderField(name = "Link", value = "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\""),
		@HeaderField(name = "Link", value = "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"") })
@AcceptPostNxPrdfSerialisations
public class RESTinterface {

	static final Logger _log = Logger.getLogger(RESTinterface.class.getName());

	@Context
	ServletContext _ctx;

	@GET
	public Response getOverview(@Context UriInfo uriinfo, @Context Request request) {
		@SuppressWarnings("unchecked")
		Map<String, Iterable<Node[]>> map = (Map<String, Iterable<Node[]>>) _ctx
				.getAttribute(DerServletContextListener.ServletContextAttributes.STORED_RDF_DATASET.name());

		EntityTag etag = new EntityTag("W/" + Integer.toHexString(map.hashCode()));

		ResponseBuilder builder = request.evaluatePreconditions(etag);
		if (builder != null)
			return builder.build();
		else {
			List<Node[]> l = new LinkedList<Node[]>();

			URI absPath = uriinfo.getAbsolutePath();
			URI base = absPath;

			Resource baseRes = new Resource(base.toString());

			l.add(new Node[] { baseRes, RDF.TYPE, LDP.BASIC_CONTAINER });
			l.add(new Node[] { baseRes, RDF.TYPE, LDP.CONTAINER });

			for (String s : map.keySet()) {
				l.add(new Node[] { baseRes, LDP.CONTAINS, new Resource(base.resolve(s).toString()) });
			}

			return Response.ok(new GenericEntity<Iterable<Node[]>>(l) {
			}).tag(etag).build();
		}
	}

	@POST
	public Response postResource(@Context UriInfo uriinfo, Iterable<Node[]> input, @Context HttpHeaders headers) {

		// We implement here the append-to-collection-resource semantics of
		// a POST request.

		// Get the RDF dataset from the servlet context.
		@SuppressWarnings("unchecked")
		Map<String, Iterable<Node[]>> map = (Map<String, Iterable<Node[]>>) _ctx
				.getAttribute(DerServletContextListener.ServletContextAttributes.STORED_RDF_DATASET.name());

		//
		// Generate a local name for the new resource
		//

		// Retrieve the current ID from the servlet context.
		AtomicInteger id = (AtomicInteger) _ctx
				.getAttribute(ServletContextAttributes.CURRENT_POSTED_RESOURCE_ID.name());

		// Considering slugs
		String newlyCreatedResourceLocalName = headers.getHeaderString("Slug") == null ? null
				: Normalizer.normalize(headers.getHeaderString("Slug"), Form.NFD).replaceAll("[^ -~]", "");

		if (newlyCreatedResourceLocalName != null && map.containsKey(newlyCreatedResourceLocalName))
			newlyCreatedResourceLocalName = null;

		if (newlyCreatedResourceLocalName == null)
			// Increment the current ID.
			// Because entries in the RDF dataset could also have been
			// created using PUT requests, we have to increment until we find a
			// free "space" in the RDF dataset.
			do {
				newlyCreatedResourceLocalName = Integer.toString(id.incrementAndGet());
			} while (map.containsKey(newlyCreatedResourceLocalName));

		//
		// Local name generated.
		//

		// Claim the space for the newly created resources in the RDF
		// dataset.
		map.put(newlyCreatedResourceLocalName, Collections.<Node[]>emptyList());

		//
		// Generate a new URI for the new resource.
		//

		// Get the URI of the collection against which to resolve the local
		// name.
		URI absPath = uriinfo.getAbsolutePath();
		URI uriOfPostRequest;
		if (!absPath.getPath().endsWith("/"))
			try {
				uriOfPostRequest = new URI(absPath.getScheme(), absPath.getAuthority(), absPath.getPath() + "/",
						absPath.getQuery(), absPath.getFragment());
			} catch (URISyntaxException e) {
				uriOfPostRequest = absPath;
			}
		else
			uriOfPostRequest = absPath;

		// resolve the local name against the collection's URI.
		URI newlyCreatedResourceURI;
		try {
			newlyCreatedResourceURI = uriOfPostRequest.resolve(new URI(null, newlyCreatedResourceLocalName, null));
		} catch (URISyntaxException e) {
			throw new BadRequestException(e);
		}

		// Process the RDF graph such that all placeholders for the newly
		// created URI get replaced by the newly created URI.
		Iterable<Node[]> reResolvedInput = new NodesReResolvingIterator(input, newlyCreatedResourceURI);

		// Save the RDF data from the incoming request in a List.
		List<Node[]> l = new LinkedList<Node[]>();
		for (Node[] nx : reResolvedInput)
			l.add(nx);

		// Store the RDF graph in the RDF dataset.
		map.put(newlyCreatedResourceLocalName, l);

		return Response.created(newlyCreatedResourceURI).build();
	}

	@DELETE
	public Response deleteEverything() {
		@SuppressWarnings("unchecked")
		Map<String, Iterable<Node[]>> map = (Map<String, Iterable<Node[]>>) _ctx
				.getAttribute(DerServletContextListener.ServletContextAttributes.STORED_RDF_DATASET.name());

		map.clear();

		return Response.noContent().build();
	}
	
	@Path("{id}")
	public Content getContentSubResource() {
		return new Content();
	};
	
	@InjectHeaders(@HeaderField(name = "Link", value = "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""))
	@NotFoundOnTrailingSlash
	public class Content {

		@GET
		public Response getResource(@PathParam("id") String id, @Context Request request) {
			@SuppressWarnings("unchecked")
			Map<String, Iterable<Node[]>> map = (Map<String, Iterable<Node[]>>) _ctx
					.getAttribute(DerServletContextListener.ServletContextAttributes.STORED_RDF_DATASET.name());

			Iterable<Node[]> nx = map.get(id);

			if (nx == null)
				throw new NotFoundException();
			else {
				EntityTag etag = new EntityTag("W/" + Integer.toHexString(nx.hashCode()));

				ResponseBuilder builder = request.evaluatePreconditions(etag);
				if (builder != null)
					return builder.build();
				else
					return Response.ok(new GenericEntity<Iterable<Node[]>>(nx) {
					}).tag(etag).build();
			}
		}

		@PUT
		public Response putResource(@PathParam("id") String id, Iterable<Node[]> input, @Context Request request) {

			@SuppressWarnings("unchecked")
			Map<String, Iterable<Node[]>> map = (Map<String, Iterable<Node[]>>) _ctx
					.getAttribute(DerServletContextListener.ServletContextAttributes.STORED_RDF_DATASET.name());

			boolean resourceOverwrittenAndNotCreated = map.containsKey(id);
			EntityTag etag = new EntityTag(
					"W/" + (resourceOverwrittenAndNotCreated ? Integer.toHexString(map.get(id).hashCode()) : 0));

			ResponseBuilder builder = request.evaluatePreconditions(etag);
			if (builder != null)
				return builder.build();
			else {
				List<Node[]> l = new LinkedList<Node[]>();

				for (Node[] nx : input)
					l.add(nx);

				map.put(id, l);

				if (resourceOverwrittenAndNotCreated)
					return Response.noContent().build();
				else
					return Response.created(URI.create(id)).build();
			}
		}

		@DELETE
		public Response deleteSomething(@PathParam("id") String id) {
			@SuppressWarnings("unchecked")
			Map<String, Iterable<Node[]>> map = (Map<String, Iterable<Node[]>>) _ctx
					.getAttribute(DerServletContextListener.ServletContextAttributes.STORED_RDF_DATASET.name());

			Object o = map.remove(id);

			if (o == null)
				throw new BadRequestException();
			else
				return Response.noContent().build();

		}
	}

}