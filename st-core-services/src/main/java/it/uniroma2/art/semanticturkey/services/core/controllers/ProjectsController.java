package it.uniroma2.art.semanticturkey.services.core.controllers;

import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.services.core.Projects;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProjectsController 
    implements ApplicationContextAware {
    ApplicationContext context;
    
	@Override
	public void setApplicationContext(ApplicationContext arg0)
			throws BeansException {
		context=arg0;
		
	}



    @RequestMapping(value = "/Projects/disconnectFromProject", method = RequestMethod.GET)
	public @ResponseBody String  disconnectFromProjectPublished(  @RequestParam(value = "consumer")it.uniroma2.art.semanticturkey.project.ProjectConsumer consumer , @RequestParam(value = "projectName")java.lang.String projectName) 
	{
		Projects fun = (Projects) context.getBean(Projects.class);
		try {

			return fun.disconnectFromProject(  consumer , projectName).getResponseContent();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}

    }


    @RequestMapping(value = "/Projects/listProjects", method = RequestMethod.GET)
	public @ResponseBody String  listProjectsPublished(@RequestParam(value = "consumer", required=false, defaultValue="SYSTEM")it.uniroma2.art.semanticturkey.project.ProjectConsumer consumer) 
	{
    	System.out.println("consumer = " + consumer);
		Projects fun = (Projects) context.getBean(Projects.class);
		try {

			return fun.listProjects(consumer).getResponseContent();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}

    }


    @RequestMapping(value = "/Projects/accessProject", method = RequestMethod.GET)
	public @ResponseBody String  accessProjectPublished(  @RequestParam(value = "consumer")it.uniroma2.art.semanticturkey.project.ProjectConsumer consumer , @RequestParam(value = "projectName")java.lang.String projectName , @RequestParam(value = "requestedAccessLevel")it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel requestedAccessLevel , @RequestParam(value = "requestedLockLevel")it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel requestedLockLevel) 
	{
		Projects fun = (Projects) context.getBean(Projects.class);
		try {

			return fun.accessProject(  consumer , projectName , requestedAccessLevel , requestedLockLevel).getResponseContent();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}

    }
}