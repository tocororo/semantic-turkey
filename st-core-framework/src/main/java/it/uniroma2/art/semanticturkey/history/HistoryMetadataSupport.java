package it.uniroma2.art.semanticturkey.history;

/**
 * A convenience class providing per-thread storage of the metadata about the currenlty executing operation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HistoryMetadataSupport {
	private static ThreadLocal<OperationMetadata> operationMetadataHolder = new ThreadLocal<>();
	
	public static void setOperationMetadata(OperationMetadata operationMetadata) {
		operationMetadataHolder.set(operationMetadata);
	}
	
	public static void removeOperationMetadata() {
		operationMetadataHolder.remove();
	}
	
	public static OperationMetadata currentOperationMetadata() {
		return operationMetadataHolder.get();
	}
	
}