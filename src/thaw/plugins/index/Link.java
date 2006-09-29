package thaw.plugins.index;

import java.sql.*;

import thaw.fcp.*;
import thaw.plugins.Hsqldb;

public class Link extends java.util.Observable {

	String indexName = null;
	String key = null;

	public Link() {

	}

	public Link(String indexName, String key) {
		this.indexName = indexName;
		this.key = key;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getKey() {
		return key;
	}
}
