package com.bank.transactions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StringUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.bank.transactions.domain.BankTransaction;

/**
 * This test demonstrates DynamoDB queryPage with filter behavior. The
 * expectation is that filter be applied on DynamoDB side and returned item
 * count should match limit if matching items are available (but it doesn't).
 */
public class QueryFilterTest {

    // TODO: Optional update this : Start
    final String proxy = "proxy.example.com";
    final int proxyPort = 8099;
    final String proxyUser = "xxx";
    final String proxyPassword = "xxx";
    // TODO: Optional update this : End
    

    final String region = "us-east-1";
    final String stackName = "test-query-filter-ddb-tables";
    final String ddbGsiDateIndexName = "accountNumber-transactionDate-index";

    final String cftFile = "src/test/resources/transacion-table-cft.yml";
    final String bootstrapDataFile = "src/test/resources/bootstrap-transactions.txt";

    AmazonCloudFormationClient cloudFormationClient;

    AmazonDynamoDBClient ddbClient;

    DynamoDBMapper dynamoDbMapper;

    ClientConfiguration clientConfig;

    AWSCredentialsProvider awsCredentialProvider = new DefaultAWSCredentialsProviderChain();

    @Before
    public void setup() throws NoSuchAlgorithmException {
        clientConfig = new ClientConfiguration();
        configureProxy();
        cloudFormationClient = AmazonCloudFormationClient.class.cast(AmazonCloudFormationClientBuilder.standard()
                .withCredentials(awsCredentialProvider).withClientConfiguration(clientConfig).withRegion(region).build());
        ddbClient = AmazonDynamoDBClient.class.cast(AmazonDynamoDBClientBuilder.standard().withCredentials(awsCredentialProvider)
                .withClientConfiguration(clientConfig).withRegion(region).build());
        dynamoDbMapper = new DynamoDBMapper(ddbClient);
    }

    @Test
    public void testQueryFilter() throws IOException, InterruptedException {
        bootstrapTable();

        /*
         * Query an index for a page of given limit size and apply filter.
         */
        BankTransaction hashKey = new BankTransaction("0000000001");
        String startDate = "2017001";
        String endDate = "2017010";
        String transactionType = "99"; // filter out
        int limit = 5;

        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":transactionTypeValue", new AttributeValue().withS(transactionType));

        DynamoDBQueryExpression<BankTransaction> queryExpression = new DynamoDBQueryExpression<BankTransaction>().withIndexName(ddbGsiDateIndexName)
                .withHashKeyValues(hashKey).withConsistentRead(false).withScanIndexForward(false)
                .withRangeKeyCondition("transactionDate",
                        new Condition().withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                                .withAttributeValueList(new AttributeValue().withS(startDate), new AttributeValue().withS(endDate)))
                .withLimit(limit).withFilterExpression("transactionType <> :transactionTypeValue").withExpressionAttributeValues(eav);

        System.out.println("Querying DynamoDB ...");
        List<BankTransaction> retrievedTransactions = dynamoDbMapper.queryPage(BankTransaction.class, queryExpression).getResults();

        System.out.println("Returned items:");
        for (BankTransaction txn : retrievedTransactions) {
            System.out.println(txn.toString());
        }

    }

    private void configureProxy() {
        if (clientConfig != null && !StringUtils.isEmpty(proxy)) {

            clientConfig.setProxyHost(proxy);
            clientConfig.setProxyPort(proxyPort);

            if (!StringUtils.isEmpty(proxy)) {
                clientConfig.setProxyUsername(proxyUser);
                clientConfig.setProxyPassword(proxyPassword);
            }
        }
    }

    private void bootstrapTable() throws IOException, InterruptedException {

        System.out.println("Bootstraping DynamoDB table ...");

        getOrCreateStack();

        List<BankTransaction> lstTransactions = getBootstrapData();
        dynamoDbMapper.batchWrite(lstTransactions, Collections.emptyList());

        System.out.println("Bootstrap completed : Loaded " + lstTransactions.size() + " items");

    }

    private Stack getOrCreateStack() throws InterruptedException, IOException {

        Stack stack = null;
        DescribeStacksRequest dsr = new DescribeStacksRequest().withStackName(stackName);

        try {
            stack = cloudFormationClient.describeStacks(dsr).getStacks().get(0);
            System.out.println("Stack already exists");
        } catch (AmazonCloudFormationException e) {

            if (e.getStatusCode() == 400) {
                // Create stack if not present
                System.out.println("Creating new stack ...");
                cloudFormationClient.createStack(
                        new CreateStackRequest().withStackName(stackName).withTemplateBody(new String(Files.readAllBytes(Paths.get(cftFile)))));
                stack = cloudFormationClient.describeStacks(dsr).getStacks().get(0);
            } else {
                throw new IllegalStateException(e);
            }
        }

        stack = cloudFormationClient.describeStacks(dsr).getStacks().get(0);
        System.out.println("Current stack status: " + stack.getStackStatus());

        // Wait for completion if required
        while (StackStatus.CREATE_IN_PROGRESS.name().equals(stack.getStackStatus())) {
            System.out.println("Waiting for stack creation to complete ...");
            Thread.sleep(10000);
            stack = cloudFormationClient.describeStacks(dsr).getStacks().get(0);
        }

        // Make sure its created
        if (StackStatus.CREATE_COMPLETE.name().equals(stack.getStackStatus())) {
            System.out.println("Stack located : " + stack.getStackId());
        } else {
            throw new IllegalStateException("Stack creation failed: " + stackName + " | Status: " + stack.getStackStatus());
        }

        return stack;

    }

    private List<BankTransaction> getBootstrapData() throws IOException {
        List<BankTransaction> lstTxn = new ArrayList<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(Paths.get(bootstrapDataFile))) {
            if (lineNumber++ == 0) {
                // Ignore header
                continue;
            }
            if (!StringUtils.isEmpty(line)) {
                String[] tokens = line.split(",");
                String accountNumber = tokens[0];
                String transactionDate = tokens[1];
                String transactionType = tokens[2];
                String transactionAmount = tokens[3];
                String recordSequence = tokens[4];
                String hashKey = buildHashKey(accountNumber, transactionDate, recordSequence);
                lstTxn.add(new BankTransaction(hashKey, accountNumber, transactionDate, transactionAmount, transactionType, recordSequence));
            }
        }
        return lstTxn;
    }

    private String buildHashKey(String accountNumber, String transactionDate, String recordSequence) throws UnsupportedEncodingException {
        // Primary key is hash of "AccountNumber_Date_Sequence"
        return accountNumber + "_" + transactionDate + "_" + recordSequence;
    }

}
