package it.uniroma2.art.semanticturkey.services;

public class Response<T> {
	private T data;

	public Response(T data) {
		this.data = data;
	}
	
	public T getResult() {
		return data;
	}
}
