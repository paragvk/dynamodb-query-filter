# dynamodb-query-filter
Test to demo DynamoDB queryPage API filter issue


# Test

Query DynamoDB transaction table using GSI to retrieve 5 transactions matching the given filter criteria


- Run JUnit test `com.bank.transactions.QueryFilterTest` that will bootstrap AWS DynamoDB table (table creation and loading test data).

- DynamoDB table is create as per Cloud Formation template `src/test/resources/transacion-table-cft.yml`

- Test data is loaded as per `src/test/resources/bootstrap-transactions.txt`

- Optionally, update proxy config before running the test.



# Expectated vs Actual

If a query for page is fired on index `accountNumber-transactionDate-index` for a page of size `5` with filter to exclude `transactionType` "99":

Expected (5 items):

	BankTransaction [tranRecId=0000000001_2017010_00000010, accountNumber=0000000001, transactionDate=2017010, transactionAmount=10.00, transactionType=11, recordSequence=00000010]
	BankTransaction [tranRecId=0000000001_2017008_00000008, accountNumber=0000000001, transactionDate=2017008, transactionAmount=8.00, transactionType=11, recordSequence=00000008]
	BankTransaction [tranRecId=0000000001_2017007_00000007, accountNumber=0000000001, transactionDate=2017007, transactionAmount=7.00, transactionType=11, recordSequence=00000007]
	BankTransaction [tranRecId=0000000001_2017005_00000005, accountNumber=0000000001, transactionDate=2017005, transactionAmount=5.00, transactionType=11, recordSequence=00000005]
	BankTransaction [tranRecId=0000000001_2017003_00000003, accountNumber=0000000001, transactionDate=2017003, transactionAmount=3.00, transactionType=11, recordSequence=00000003]



Actual (3 items):

	BankTransaction [tranRecId=0000000001_2017010_00000010, accountNumber=0000000001, transactionDate=2017010, transactionAmount=10.00, transactionType=11, recordSequence=00000010]
	BankTransaction [tranRecId=0000000001_2017008_00000008, accountNumber=0000000001, transactionDate=2017008, transactionAmount=8.00, transactionType=11, recordSequence=00000008]
	BankTransaction [tranRecId=0000000001_2017007_00000007, accountNumber=0000000001, transactionDate=2017007, transactionAmount=7.00, transactionType=11, recordSequence=00000007]



# Ask

When asking for `limit` number of items using `queryPage` API, DynamoDB should continue seeking records until `limit` number of matched items are found. This helps in implementing `pagination with filter` effectively. Current work around is to pull more than required number of items eagerly or make multiple calls with subsequent `exclusiveStartKey` until required number of items are found (which is inefficient).


