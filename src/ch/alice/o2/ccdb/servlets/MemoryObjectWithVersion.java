package ch.alice.o2.ccdb.servlets;

import java.io.File;

import myjava.com.github.dosarudaniel.gsoc.Blob;

public class MemoryObjectWithVersion extends LocalObjectWithVersion {
    private Blob blob;

    public MemoryObjectWithVersion(long startTime, File entry, Blob blob) {
	super(startTime, entry);
	this.blob = blob;
    }
}
