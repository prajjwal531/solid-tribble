package org.meghana.creditcardapi.transactions.dao;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.sql.DataSource;

import org.meghana.creditcardapi.transactions.model.Account;
import org.meghana.creditcardapi.transactions.model.AccountDAO;
import org.meghana.creditcardapi.transactions.model.Ledger;
import org.meghana.creditcardapi.transactions.model.Transaction;
import org.meghana.creditcardapi.transactions.model.transcationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class AccountDaoImpl implements AccountDAO {
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplateObject;

	public void setDataSource(DataSource ds) {
		this.dataSource = dataSource;
		this.jdbcTemplateObject = new JdbcTemplate(dataSource);
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	@Override

	public String create(Account ac) {

		String SQL = "insert into account (name, dob, ssn, id,amount) values (?, ?,?,?,?)";
		String dob = ac.getDob().replace("/", "");
		String Id = Integer.toString(ac.getSsn()) + dob;
		jdbcTemplateObject.update(SQL, ac.getName(), ac.getDob(), ac.getSsn(), Id, ac.getAmount());
		Transaction t = new Transaction(Id, "Account Creation", ac.getAmount());
		this.ImplementTransaction(t);
		return Id;
	}

	public String ImplementTransaction(Transaction t) {
		String SQL = "insert into transaction (transactionid, transactiontype, amount, accountid) values (?,?,?,?)";
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(Calendar.getInstance().getTime());
		String TransactionId = t.getId() + timeStamp.replaceAll("[-:]", "");
		jdbcTemplateObject.update(SQL, TransactionId, t.getTransactiontype(), t.getAmount(), t.getId());
		Ledger l = new Ledger(t.getId(), t.getTransactiontype(), t.getAmount(), TransactionId);
		this.ImplementLedger(l);
		return TransactionId;
	}

	public String applyRule(String transacationType) {

		if (transacationType == new transcationType().credit)
			return new transcationType().debit;
		else if (transacationType == new transcationType().debit)
			return new transcationType().credit;
		return transacationType;
	}

	public void ImplementLedger(Ledger l) {
		String SQL = "insert into userledger (transactionid, transactiontype, amount, accountid) values (?,?,?,?)";
		jdbcTemplateObject.update(SQL, l.getTransactionid(), l.getTransactiontype(), l.getAmount(), l.getId());
		SQL = "insert into bankledger (transactionid, transactiontype, amount, accountid) values (?,?,?,?)";
		String TransactionType = this.applyRule(l.getTransactiontype());
		jdbcTemplateObject.update(SQL, l.getTransactionid(), TransactionType, l.getAmount(), l.getId());

	}

	public String purchase(Transaction t) {

		int amount = this.jdbcTemplateObject.queryForObject("select amount from account where id=?",
				new Object[] { t.getId() }, Integer.class);
		double amt = t.getAmount();
		int am1 = (int) amt;
		String SQL = "update account set ammount=  (transactionid, transactiontype, amount, accountid) values (?,?,?,?)";
		this.jdbcTemplateObject.update("update account set amount= ? where id =?", (amount - am1), t.getId());
		String tId = this.ImplementTransaction(t);
		return tId;

	}

	@Override
	public Account getAccountdetails(String id) {
		String SQL = "select * from account where id = ?";
		Account account = jdbcTemplateObject.queryForObject(SQL, new Object[] { id }, new AccountMapper());
		return account;
	}

	@Override
	public List<Transaction> getTransaction(String id) {
		String SQL = "SELECT transactionid,accountid, transactiontype, amount, transactiontime FROM transaction where accountid = ? ORDER BY transactiontime DESC";
		List<Transaction> transaction = jdbcTemplateObject.query(SQL, new Object[] { id }, new TransactionMapper());

		return transaction;
	}

}
