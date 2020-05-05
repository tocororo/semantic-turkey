package it.uniroma2.art.semanticturkey.zthes;

public class ZthesSerializationException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6015086539225428409L;

	public ZthesSerializationException(){
		super();
	}
	
	public ZthesSerializationException(String msg){
		super(msg);
	}

	public ZthesSerializationException(Throwable e){
		super(e);
	}

}
