package com.bank.transactions.domain;

import java.io.Serializable;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.document.Item;

@DynamoDBTable(tableName = "test-transactions")
public class BankTransaction extends Item implements Serializable {

    private static final long serialVersionUID = -2495576370749704310L;

    private String tranRecId;
    private String accountNumber;
    private String transactionDate;
    private String transactionAmount;
    private String transactionType;
    private String recordSequence;

    public BankTransaction() {
        super();
    }

    public BankTransaction(String accountNumber) {
        super();
        this.accountNumber = accountNumber;
    }

    public BankTransaction(String transRecId, String accountNum, String transactionDate, String transactionAmount, String transactionType,
            String recordSequence) {
        super();
        this.tranRecId = transRecId;
        this.accountNumber = accountNum;
        this.transactionDate = transactionDate;
        this.transactionAmount = transactionAmount;
        this.transactionType = transactionType;
        this.recordSequence = recordSequence;
    }

    @DynamoDBHashKey
    public String getTranRecId() {
        return tranRecId;
    }

    public void setTranRecId(String transRecId) {
        this.tranRecId = transRecId;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexNames = {"accountNumber-transactionDate-index", "accountNum-transactionType-index"})
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "accountNumber-transactionDate-index")
    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(String transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getRecordSequence() {
        return recordSequence;
    }

    public void setRecordSequence(String recordSequence) {
        this.recordSequence = recordSequence;
    }

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "accountNum-transactionType-index")
    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public String toString() {
        return "BankTransaction [tranRecId=" + tranRecId + ", accountNumber=" + accountNumber + ", transactionDate=" + transactionDate
                + ", transactionAmount=" + transactionAmount + ", transactionType=" + transactionType + ", recordSequence=" + recordSequence + "]";
    }

}
