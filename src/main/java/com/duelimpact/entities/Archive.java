package com.duelimpact.entities;

public class Archive {

	private String source;
	private String destination;
	private String buildDestination;

	public Archive() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Archive(String source, String destination, String buildDestination) {
		super();
		this.source = source;
		this.destination = destination;
		this.buildDestination = buildDestination;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((buildDestination == null) ? 0 : buildDestination.hashCode());
		result = prime * result + ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Archive other = (Archive) obj;
		if (buildDestination == null) {
			if (other.buildDestination != null)
				return false;
		} else if (!buildDestination.equals(other.buildDestination))
			return false;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getBuildDestination() {
		return buildDestination;
	}

	public void setBuildDestination(String buildDestination) {
		this.buildDestination = buildDestination;
	}

	@Override
	public String toString() {
		return "Archive [source=" + source + ", destination=" + destination + ", buildDestination=" + buildDestination
				+ "]";
	}
}
