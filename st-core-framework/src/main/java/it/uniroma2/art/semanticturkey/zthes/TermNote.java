package it.uniroma2.art.semanticturkey.zthes;

public class TermNote {
	
	public enum NoteLabel { Definition }
	
	public static class Attr {
		public static final String LABEL = "label";
	}

	private String note; //content
	private NoteLabel label; //attr
	
	public TermNote(String note) {
		this.note = note;
	}
	
	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public NoteLabel getLabel() {
		return label;
	}

	public void setLabel(NoteLabel label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return this.note + "\n" +
				Attr.LABEL + " " + this.label;
	}
	
}
