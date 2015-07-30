package util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class LuceneInstanceListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("LuceneInstanceListener called");
		
		LuceneInstanceCreator.init();
	}
	
}
