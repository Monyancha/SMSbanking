package com.khizhny.smsbanking.model;

import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class Bank  implements java.io.Serializable{

	  private int id=-1;  // id in main DB. If -1 then it is new.
	  public static final long serialVersionUID = 3;
	  private int editable=1; // 1 if user is allowed to modify
	  private int active=1;  // 1 indicates that user want to watch this account info in program. test
	  private String name;
	  private String phone;
    private String country=null;
  	private String defaultCurrency;
	  private BigDecimal currentAccountState=new BigDecimal("0.00"); // used to keep last account state in db for widgets.
    public List<Rule> ruleList= new ArrayList<>();

    public Bank(){  // default constructor
	}

    /**
     * Constructor is used to clone BankV2 Object from template with all subRules.
     * @param originBank - Bank object to be copy.
     */
	public Bank(Bank originBank) {
		this.name = originBank.name;
        this.phone = originBank.phone;
        this.country = originBank.country;
		    this.defaultCurrency = originBank.defaultCurrency;
		// Cloning all rules
		for (Rule r : originBank.ruleList) {
			this.ruleList.add( new Rule(r, this));
		}
	}



	public int getId() {
		return id;
	}

	@NonNull
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
    public boolean isEditable() {
        return editable != 0;
    }

    public String toString(){
		return name;
	}

	/**
	 * Sets bank id and changes all bankid in subRules.
	 * @param id ID
	 */
	public  void setId(int id){
		this.id=id;
	}
    public void setEditable(int editable){
		this.editable=editable;
	}

    public void setActive(int active){
		this.active=active;
	}

    public  void setName(String name){
        this.name=name.replaceAll("'", "");
    }

    public  void setCountry(String country){
        this.country=country;
    }

    public void setPhone(String phone){
		this.phone=phone.replace("'", "");
	}

    public void setDefaultCurrency(String defaultCurrency){
		this.defaultCurrency =defaultCurrency;
	}

	/**
	 * Function will export all BankV2 settings including Rules and subrules to a file.
	 * @param b BankV2 object which is exported.
	 * @param filePath File path where BankV2 should be exported
	 * @return True if success, False if failed.
	 */
			public static Uri exportBank(Bank b, String filePath){
						try{
								b.setCurrentAccountState("0");
								ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(filePath)));
								oos.writeObject(b);
								oos.flush();
								oos.close();
							return Uri.parse("file:///"+filePath);
						} catch (IOException e) {
								Log.v(LOG,"Serialization Save Error : "+ e.getMessage());
								e.printStackTrace();
								return null;
						}
			}

		public static void impersonalize(Bank bank){
				for (Rule r:bank.ruleList) {
						Rule.impersonalize(r);
				}
		}

	/**
	 * These function reads Object from file
	 * @param filePath File path
	 * @return Object read from file or null.
	 */
	@Nullable
	public static Bank importBank(String filePath)
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(filePath)));
            Bank b=(Bank) ois.readObject();
            b.setCurrentAccountState("0");
            return b;
		}
		catch(InvalidClassException e) {
			Log.v(LOG,e.getMessage());
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

    public ContentValues getContentValues(){
		ContentValues v = new ContentValues();
		if (id>=1) v.put("_id",id);
		v.put("name",name);
		v.put("phone",phone);
		v.put("active", active);
		v.put("default_currency", defaultCurrency);
		v.put("editable",editable);
        v.put("country",country);
		v.put("current_account_state",currentAccountState.toString());
		return v;
	}

    public void setCurrentAccountState(BigDecimal currentAccountState) {
		this.currentAccountState = currentAccountState;
	}


    public void setCurrentAccountState(String currentAccountState) {
		this.currentAccountState = new BigDecimal(currentAccountState.replace(",", ".")).setScale(2, BigDecimal.ROUND_HALF_UP);
	}

     public String getCurrentAccountState() {
		return currentAccountState.toString();
	}
}
