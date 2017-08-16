package cs8674.core;


public class Conference {
	private String volume;
    private String ee;
    private String editor;
    private String mdate;
    private int year;
    private String publisher;
    private String title;
	private String key;
	private String isbn;
	private String bookTitle;
	
	/**
	 * Default constructor
	 */
	public Conference() {
	
	}
	
	/**
	 * returns isbn
     * @return: returns isbn
     */
	public String getIsbn() {
		return isbn;
	}

	/**
	 * sets isbn
	 * @param isbn : isbn to be set
	 */
	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	/**
     * getter of boot title
     * @return: returns book title of this conference
     */
	public String getBookTitle() {
		return bookTitle;
	}

	/**
	 * sets boot title
	 * @param bookTitle : book title of the conference
	 */
	
	public void setBookTitle(String bookTitle) {
		this.bookTitle = bookTitle;
	}
	
	/**
     * getter of conference volume
     * @return: returns volume of the conference
     */
	public String getVolume() {
		return volume;
	}
	
	/**
	 * sets the volume
	 * @param volume : volume of the conference
	 */
	
	public void setVolume(String volume) {
		this.volume = volume;
	}
	
	/**
     * getter
     * @return: returns ee of the conference
     */
	public String getEe() {
		return ee;
	}
	
	/**
	 * sets ee
	 * @param ee :ee of the conference
	 */
	public void setEe(String ee) {
		this.ee = ee;
	}
	
	/**
     * getter
     * @return: returns editor of the conference
     */
	public String getEditor() {
		return editor;
	}
	
	/**
	 * sets editor
	 * @param editor : editor of the conference
	 */
	public void setEditor(String editor) {
		this.editor = editor;
	}
	
	/**
     * getter
     * @return: returns M date of the conference
     */
	public String getMdate() {
		return mdate;
	}
	
	/**
	 * sets M date
	 * @param mdate : M date of the conference
	 */
	public void setMdate(String mdate) {
		this.mdate = mdate;
	}
	
	/**
	 * returns year
	 * @return : year of the conference
	 */
	public int getYear() {
		return year;
	}
	/**
	 * sets year
	 * @param year : year of the conference
	 */
	public void setYear(int year) {
		this.year = year;
	}
	
	/**
     * get publisher
     * @return: returns publisher of the conference
     */
	public String getPublisher() {
		return publisher;
	}
	
	/**
	 * set publisher
	 * @param publisher : publisher of the conference
	 */
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	
	/**
     * getter
     * @return: returns title of the conference
     */
	public String getTitle() {
		return title;
	}
	
	/**
	 * sets title
	 * @param title : title of the conference
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
     * getter of key
     * @return: returns key of the conference/journal, key has data whether this is a
     * 			journal or conference
     */
	public String getKey() {
		return key;
	}
	
	/**
	 * set key of the conference/journal
	 * @param key : key of the conference/journal
	 */
	public void setKey(String key) {
		this.key = key;
	}
}
