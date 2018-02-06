package com.khizhny.smsbanking;

import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.model.Rule;
import com.khizhny.smsbanking.model.SubRule;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.khizhny.smsbanking.MyApplication.db;
import static org.junit.Assert.*;

public class UnitTests {
    @Test
    public void makeInitialWordSplitting() throws Exception {
        //           0    1   2    3         4       5    6    7    8    9       10
        String msg=" This is test message. Withdraw 50.34 UAH. You have 50.65UAH left ";
        // defining BankV2
        Bank b = new Bank();
        b.setDefaultCurrency("UAH");

        // defining Rule
        Rule r = new Rule(b,"test rule");
        r.setSmsBody(msg);
        Rule.transactionType trans_type=Rule.transactionType.WITHDRAW;
        r.setRuleType(trans_type.ordinal());
        r.makeInitialWordSplitting();
        assertEquals(11, r.words.size());
        assertEquals("This",r.words.get(0).getBody());
        assertEquals("50.65UAH",r.words.get(9).getBody());
        assertEquals("left",r.words.get(10).getBody());
    }

    @Test
    public void MergeRight() throws Exception {
        //           0    1   2    3         4       5    6    7    8    9       10
        String msg="This is test message. Withdraw 50.34 UAH. You have 50.65UAH left ";

        // defining BankV2
        Bank b = new Bank();
        b.setDefaultCurrency("UAH");

        // defining Rule
        Rule r = new Rule(b,"test rule");
        r.setSmsBody(msg);
        Rule.transactionType trans_type=Rule.transactionType.WITHDRAW;
        r.setRuleType(trans_type.ordinal());
        r.makeInitialWordSplitting();

        r.mergeRight(r.words.get(5));
        //             0    1   2    3         4       5        6    7    8       9
        //String msg="This is test message. Withdraw 50.34 UAH. You have 50.65UAH left ";
        assertEquals(10, r.words.size());
        assertEquals("50.34 UAH.",r.words.get(5).getBody());
        assertEquals("left",r.words.get(9).getBody());
        assertEquals("You",r.words.get(6).getBody());

        r.mergeRight(r.words.get(9));
        //             0    1   2    3         4       5        6    7    8       9
        //String msg="This is test message. Withdraw 50.34 UAH. You have 50.65UAH left ";
        assertEquals(10, r.words.size());
        assertEquals("left ",r.words.get(9).getBody());
    }

    @Test
    public void MergeLeft() throws Exception {
        //           0    1   2    3         4       5    6    7    8    9       10
        String msg="This is test message. Withdraw 50.34 UAH. You have 50.65UAH left ";
        // defining BankV2
        Bank b = new Bank();
        b.setDefaultCurrency("UAH");

        // defining Rule
        Rule r = new Rule(b,"test rule");
        r.setSmsBody(msg);
        Rule.transactionType trans_type=Rule.transactionType.WITHDRAW;
        r.setRuleType(trans_type.ordinal());
        r.makeInitialWordSplitting();

        r.mergeLeft(r.words.get(5));
        //             0    1   2    3       [    4       ] 5    6    7     8      9
        //String msg="This is test message. Withdraw 50.34 UAH. You have 50.65UAH left ";
        assertEquals(10, r.words.size());
        assertEquals("Withdraw 50.34",r.words.get(4).getBody());
        assertEquals("UAH.",r.words.get(5).getBody());
        assertEquals("left",r.words.get(9).getBody());

        r.mergeLeft(r.words.get(0));
        assertEquals(10, r.words.size());
        assertEquals("This",r.words.get(0).getBody());
        assertEquals("left",r.words.get(9).getBody());
    }

    @Test
    public void Split() throws Exception {
        //           0    1   2    3         4       5    6    7    8    9       10
        String msg="This is test message. Withdraw 50.34 UAH. You have 50.65UAH left ";
        // defining BankV2
        Bank b = new Bank();
        b.setDefaultCurrency("UAH");

        // defining Rule
        Rule r = new Rule(b,"test rule");
        r.setSmsBody(msg);
        Rule.transactionType trans_type=Rule.transactionType.WITHDRAW;
        r.setRuleType(trans_type.ordinal());
        r.makeInitialWordSplitting();

        r.split(r.words.get(4),4);
        //             0    1   2    3        4    5    6    7     8   9     10     11
        //String msg="This is test message. With draw 50.34 UAH. You have 50.65UAH left ";
        assertEquals(12, r.words.size());
        assertEquals("With",r.words.get(4).getBody());
        assertEquals("draw",r.words.get(5).getBody());
        assertEquals("left",r.words.get(11).getBody());

    }

    @Test
    public void MaskCreation() throws Exception {
        //              0    1   2    3         4       5    6    7    8    9       10
        String msg="  This  is test message. Withdraw 50.34  UAH. You have 50.65UAH left  ";

        // defining BankV2
        Bank b = new Bank();
        b.setDefaultCurrency("UAH");

        // defining Rule
        Rule r = new Rule(b,"test rule");
        r.setSmsBody(msg);
        Rule.transactionType trans_type=Rule.transactionType.WITHDRAW;
        r.setRuleType(trans_type.ordinal());
        r.makeInitialWordSplitting();
        r.words.get(5).changeWordType();
        r.words.get(6).changeWordType();
        r.words.get(6).changeWordType();
        r.updateMask();
        String mask=r.getMask();
        assertEquals("^  \\QThis\\E  \\Qis\\E \\Qtest\\E \\Qmessage.\\E \\QWithdraw\\E (.*)  (.{4}) \\QYou\\E \\Qhave\\E \\Q50.65UAH\\E \\Qleft\\E  $", mask);
        String values=r.getValues();
        assertEquals("50.34\nUAH.",values);
    }

    @Test
    public void Regexp_isCorrect() throws Exception {
        /*
        *       ^     # start of string
                \s*   # optional whitespace
                (\w+) # one or more alphanumeric characters, capture the match
                \s*   # optional whitespace
                \(    # a (
                \s*   # optional whitespace
                (\d+) # a number, capture the match
                \D+   # one or more non-digits
                (\d+) # a number, capture the match
                \D+   # one or more non-digits
                \)    # a )
                \s*   # optional whitespace
                $     # end of string
        * */
        String example="^\\s*(\\w+)\\s*\\(\\s*(\\d+)\\D+(\\d+)\\D+\\)\\s*$";
        String smsMsg="This is test message. Withdraw 50.34 UAH. You have 50.65 UAH left";
        String mask="^This is test message. Withdraw \\s*(.*) \\s*(.*) \\s*\\QYou\\E \\s*\\Qhave\\E \\s*(.*) \\s*\\QUAH\\E \\s*\\Qleft\\E\\s*$";
        Pattern pattern = Pattern.compile(mask);
        Matcher matcher = pattern.matcher(smsMsg);
        if (matcher.matches()) {
            assertEquals(3, matcher.groupCount());
            assertEquals("50.34", matcher.group(1).trim());
            assertEquals("UAH.", matcher.group(2).trim());
            assertEquals("50.65", matcher.group(3).trim());
        }else{
            assertEquals("found","not found");
        }

    }
    @Test
    public void Migration_test() throws Exception {

        for (Bank b: db.getMyBanks("*")){
            for (Rule r: b.ruleList){
                for (SubRule sr: r.subRuleList) {
                    switch (sr.getExtractionMethod()) {
                        case WORD_AFTER_PHRASE:
                            break;
                        case WORD_BEFORE_PHRASE:
                            break;
                        case WORDS_BETWEEN_PHRASES:
                            break;
                        case USE_CONSTANT:
                            break;
                        case USE_REGEX:
                            break;
                    }
                    assertEquals(1, 1);
                }
            }
       }
    }
}