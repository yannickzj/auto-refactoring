package clone.analysis;

public interface Matchable extends Comparable<Matchable>{
	
	public String getPackageName();
	public String getClassName();
	public String getQualifiedMethodName();
	public String getSourceLine();
	public String getLogLine();
	public int size();
	public int compareSize();

}
