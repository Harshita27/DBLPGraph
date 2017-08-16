package cs8674.core;

public class Author {
    String authorId;
    String name;
    int citationCount=0;
    
    /**
     * returns name of the coAuthors
     * @return name : name of the coAuthors
     */
    public String getName() {
        return name;
    }

    /**
     *  sets coAuthors name
     * @param name : name of the coAuthors
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * returns citation count
     * @return citationCount : citation count of the coAuthors
     */
    public int getCitationCount() {
        return citationCount;
    }

    
    /**
     * sets citationCount
     * @param citationCount : citation count of the coAuthors
     */
    public void setCitationCount(int citationCount) {
        this.citationCount += citationCount;
    }

}
