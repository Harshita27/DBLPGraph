package cs8674.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Committee {
	
	List<String> members;
	String name;
	String fullName;
	Integer year;
	HashMap<String, String> hash;
	
	/**
	 * Constructor, initializes the hash map 
	 */
	public Committee() {
		hash = new HashMap<String, String>();
		members = new ArrayList<String>();
		hash.put("ase", "Automated Software Engineering"); 
		hash.put("fse","Foundations of Software Engineering"); 
		hash.put("icfp", "International Conference on Functional Programming");
		hash.put("ecoop", "European Conference on Object-Oriented Programming");
		hash.put("esop", "European Symposium on Programming");
		hash.put("icse", "International Conference on Software Engineering");
		hash.put("ismm", "International Symposium on Memory Management");
		hash.put("issta", "International Symposium on Software Testing and Analysis");
		hash.put("oopsla",  "Object-Oriented Programming, Systems, Languages & Applications");
		hash.put("pldi", "Programming Language Design and Implementation");
		hash.put("popl",  "Principles of Programming Languages");
		hash.put("ppopp",  "Principles and Practice of Parallel Programming");
	}
	
	/**
	 * adds the given member to the list of members
	 * @param member : member of this committee
	 */
	public void addMember(String member) {
	
		members.add(member);
	}
	
	/**
	 * returns list of members
	 * @return : returns the list of all the members
	 */
	public List<String> getMembers() {
		return members;
	}

	/**
	 * returns name
	 * @return name : returns the name of the committee
	 */
	public String getName() {
		return name;
	}

	/**
	 * sets the name of the committee given the short form
	 * @param name : name of the committee in short form
	 */
	public void setName(String name) {
		this.name = name;
		this.fullName = hash.get(name);
	}

	/**
	 * returns full name
	 * @return fullName : returns the full name of the committee
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * returns year
	 * @return year : returns the year of the committee
	 */
	public Integer getYear() {
		return year;
	}

	/**
	 * set the year of committee
	 * @param year : year if the committee
	 */
	public void setYear(Integer year) {
		this.year = year;
	}

	/**
	 * returns the parameters and their  values in string format
	 * @return String: returns the parameters and their values in string format
	 */
	@Override
	public String toString() {
		return "Committee [members=" + members + ", name=" + name + ", fullName=" + fullName + ", year=" + year + "]";
	}	
}
