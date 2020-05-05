package it.uniroma2.art.semanticturkey.servlet;

public interface ResponseException extends ResponseProblem {
	
	public void setStackTrace(Exception e);
	
	public String getStackTrace();

}
