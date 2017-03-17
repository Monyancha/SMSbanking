package com.khizhny.smsbanking;

import android.content.ContentValues;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static com.khizhny.smsbanking.MyApplication.LOG;

public class Bank  implements java.io.Serializable{

	// Information   about your bank account
	private int id;  // id in main DB. If -1 then it is new.
	static final long serialVersionUID = 1; // Is used to indicate class version during Import/Export
	private int editable; // true if user is allowed to modify
	private int active;  // indicates that user want to watch this account info in program. test
	private String name;
	private String phone;
	private String defaultCurrency;
	private BigDecimal currentAccountState; // used to keep last account state in db for widgets.
	List<Rule> ruleList;

	/**
	 * Bank object constructor
	 */
	Bank(){  // default constructor
		id=-1;  // indicates that object is not in the main DB
		editable=1;
		active=1;
        ruleList=new ArrayList<Rule>();
		currentAccountState=new BigDecimal("0.00");
	}

    /**
     * Constructor is used to clone Bank Object from template with all subrules.
     * @param origin - Bank object to be copy.
     */
	public Bank(Bank origin) {
		this.id=-1;
		this.editable=1;
		this.active=1;
		this.name = origin.name;
		this.phone = origin.phone;
		this.defaultCurrency = origin.defaultCurrency;
		currentAccountState=new BigDecimal("0.00");
		for (Rule r : origin.ruleList) {
			this.ruleList.add( new Rule(r,-1));
		}

	}

	public int getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getPhone() {
		return phone;
	}
	public String getDefaultCurrency() {
		return defaultCurrency;
	}
	public boolean isActive() {
		return active != 0;
	}

	public String toString(){
		return name;
	}

	/**
	 * Sets bank id and changes all bankid in subrules.
	 * @param id ID
	 */
	public  void setId(int id){
		this.id=id;
		if (ruleList!=null) {
			for (Rule r : ruleList) {
				r.changeBankId(id);
			}
		}
	}
	public  void setEditable(int editable){
		this.editable=editable;
	}

	public  void setActive(int active){
		this.active=active;
	}
	public  void setName(String name){
		this.name=name.replaceAll("'", "");
	}
	public  void setPhone(String phone){
		this.phone=phone.replace("'", "");
	}
	public  void setDefaultCurrency(String defaultCurrency){
		this.defaultCurrency =defaultCurrency;
	}

	/**
	 * Function will export all Bank settings including Rules and subrules to a file.
	 * @param b Bank object which is exported.
	 * @param filePath File path where Bank should be exported
	 * @return True if success, False if failed.
	 */
	static Boolean exportBank(Bank b, String filePath){
		try{
			//filePath="/storage/sdcard0/test.dat";  // used for testing while usb storage is mountd
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(filePath)));
			oos.writeObject(b);
			oos.flush();
			oos.close();
			return true;
		} catch (IOException e) {
			Log.v(LOG,"Serialization Save Error : "+ e.getMessage());
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * These function reads OBject from file
	 * @param filePath File path
	 * @return Object read from file or null.
	 */
	static Object importBank(String filePath)
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(filePath)));
			return ois.readObject();
		}
		catch(Exception e)
		{
			Log.v(LOG,e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	ContentValues getContentValues(){
		ContentValues v = new ContentValues();
		if (id>=1) v.put("_id",id);
		v.put("name",name);
		v.put("phone",phone);
		v.put("active", active);
		v.put("default_currency", defaultCurrency);
		v.put("editable",editable);
		v.put("current_account_state",currentAccountState.toString());
		return v;
	}

	void setCurrentAccountState(BigDecimal currentAccountState) {
		this.currentAccountState = currentAccountState;
	}

	void setCurrentAccountState(String currentAccountState) {
		this.currentAccountState = new BigDecimal(currentAccountState.replace(",", ".")).setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	String getCurrentAccountState() {
		return currentAccountState.toString();
	}
}
