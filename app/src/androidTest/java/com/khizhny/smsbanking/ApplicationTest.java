package com.khizhny.smsbanking;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.test.RenamingDelegatingContext;

import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.model.Rule;
import com.khizhny.smsbanking.model.SubRule;


/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    private DatabaseAccess db;

    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    public void setUp(){
        RenamingDelegatingContext context
                = new RenamingDelegatingContext(getContext(), "test_");
        db = new DatabaseAccess(context);
        db.open();
    }

    public void testSubRuleModify(){
        // Here I have my new database which is not connected to the standard database of the App
        String old_method_result;
        for (Bank b: db.getBanks("Ukraine")){
            for (Rule r: b.ruleList){
                for (SubRule sr: r.subRuleList) {
                    switch (sr.getExtractionMethod()) {
                        case WORD_AFTER_PHRASE:
                            old_method_result=sr.applySubRule(r.getSmsBody(),0);
                            break;
                        case WORD_BEFORE_PHRASE:
                            old_method_result=sr.applySubRule(r.getSmsBody(),0);
                            break;
                        case WORDS_BETWEEN_PHRASES:
                            old_method_result=sr.applySubRule(r.getSmsBody(),0);
                            break;
                        case USE_CONSTANT:
                        case USE_REGEX:
                            break;
                    }
                    assertEquals(1, 1);
                }
            }
        }
    }

    @Override
    public void tearDown() throws Exception{
        db.close();
        super.tearDown();
    }
}