package ch.alice.o2.ccdb.servlets;

import java.io.File;

import myjava.com.github.dosarudaniel.gsoc.Blob;

public class MemoryObject extends LocalObjectWithVersion {
    private Blob blob;

    public Blob getBlob() {
	return blob;
    }

    public void setBlob(Blob blob) {
	this.blob = blob;
    }

    public MemoryObject(long startTime, File entry, Blob blob) {
	super(startTime, entry);
	this.blob = blob;
    }
}
