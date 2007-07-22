package thaw.plugins.miniFrost.frostKSK;

public class KSKSubMessage
	implements thaw.plugins.miniFrost.interfaces.SubMessage {


	private KSKAuthor author;
	private java.util.Date date;
	private String msg;

	public KSKSubMessage(KSKAuthor author,
			     java.util.Date date,
			     String msg) {
		this.author = author;
		this.date = date;
		this.msg = msg;
	}


	public thaw.plugins.miniFrost.interfaces.Author getAuthor() {
		return author;
	}

	protected void setAuthor(KSKAuthor author) {
		this.author = author;
	}

	protected void setDate(java.util.Date date) {
		this.date = date;
	}

	public java.util.Date getDate() {
		return date;
	}

	public String getMessage() {
		return msg;
	}
}
