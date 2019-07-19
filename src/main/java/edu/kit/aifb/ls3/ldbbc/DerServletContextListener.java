package edu.kit.aifb.ls3.ldbbc;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.HttpMethod;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;

@WebListener
public class DerServletContextListener implements ServletContextListener {

	final static Logger _log = Logger.getLogger(DerServletContextListener.class
			.getName());

	ServletContext _ctx;

	enum ServletContextAttributes {

		/**
		 * The RWLDresources servlet stores a RDF dataset. This enum constant's
		 * name represents the key, behind which the RDF dataset is stored in
		 * the {@link ServletContext}. In the {@link ServletContext}, the RDF
		 * dataset is stored as
		 * <code>Map&lt;String,Iterable&lt;Node[]&gt;&gt;</code>.
		 */
		STORED_RDF_DATASET,
		/**
		 * This enum constant's name represents the key in the
		 * {@link ServletContext}, whose value represents the ID of the resource
		 * that has last been created in a POST request. The ID is stored as
		 * {@link AtomicInteger}.
		 */
		CURRENT_POSTED_RESOURCE_ID,
		/**
		 * This enum constant's name represents the key in the {@link ServletContext},
		 * whose value represents a map from {@link AtomicInteger} to a
		 * {@link LinkedList} of RDF terms, ie. instances of {@link Node}.
		 */
		LISTS,
		/**
		 * This enum constant's name represents the key in the {@link ServletContext},
		 * whose value represents the ID of the list resource that has last been created
		 * in a POST request. The ID is stored as {@link AtomicInteger}.
		 */
		CURRENT_POSTED_LIST_ID
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		_ctx = sce.getServletContext();

		// Register Servlet
		ServletRegistration sr = _ctx.addServlet("A LDP container with RDF resources to read and write",
				org.glassfish.jersey.servlet.ServletContainer.class);
		sr.addMapping("/*");
		sr.setInitParameter(org.glassfish.jersey.server.ServerProperties.PROVIDER_PACKAGES,
				this.getClass().getPackage().getName() + ","
						+ org.semanticweb.yars.jaxrs.JerseyAutoDiscoverable.class.getPackage().getName());

		FilterRegistration fr;
		// Register and configure filter to handle CORS requests
		fr = _ctx.addFilter("cross-origin", org.eclipse.jetty.servlets.CrossOriginFilter.class.getName());
		fr.setInitParameter(org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_METHODS_PARAM,
				HttpMethod.GET + "," + HttpMethod.PUT + "," + HttpMethod.POST + "," + HttpMethod.DELETE);
		fr.addMappingForUrlPatterns(null, true, "/*");
		
		// Initialise the two data strucutres that we need for the functionality
		// of this servlet in the servlet context.
		_ctx.setAttribute(ServletContextAttributes.STORED_RDF_DATASET.name(),
				new ConcurrentHashMap<String, Map<String, Set<Nodes>>>());

		_ctx.setAttribute(
				ServletContextAttributes.CURRENT_POSTED_RESOURCE_ID.name(),
				new AtomicInteger());

		_ctx.setAttribute(ServletContextAttributes.LISTS.name(),
				new ConcurrentHashMap<AtomicInteger, LinkedList<Node>>());

		_ctx.setAttribute(ServletContextAttributes.CURRENT_POSTED_LIST_ID.name(), new AtomicInteger());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub

	}

}
