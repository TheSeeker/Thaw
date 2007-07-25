package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;

import thaw.plugins.Hsqldb;


public interface KSKAttachment
	extends thaw.plugins.miniFrost.interfaces.Attachment {

	public void   insert(Hsqldb db, int messageId);
}
