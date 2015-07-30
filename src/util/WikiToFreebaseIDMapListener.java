package util;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class WikiToFreebaseIDMapListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("WikiToFreebaseIDMapListener called");
		String val = event.getServletContext().getInitParameter("fbdataset_path");
		System.out.println("value=" + val);
		try {
			WikiToFreebaseIDMap.init(val);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("wiki map size:" + WikiToFreebaseIDMap.getInstance().mapping.size());
	}
	
}
