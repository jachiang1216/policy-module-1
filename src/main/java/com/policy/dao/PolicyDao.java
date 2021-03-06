/*
 * This is a Data Access Object. It will communicate 
 * with the Database and return data. More specifically, 
 * this class will associate itself with the Policy Table.
 * @author					Domenic Gareffa
 * @version      			
 * @Class name				PolicyDao
 * @Creation Date			3:02pm on August 15, 2018
 * @History
 * @Reviewed by & Status	Patrick
 */

package com.policy.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.policy.data.Nominee;
import com.policy.data.Policy;

/**
 * NOTE: We have added a column to the existing Policy Table, 
 *  we have added sum_assured_min
 * 	and altered sum_assured to sum_assured_min
 * 
 * create table Policies (
	policy_id int primary key,
	policy_type varchar(255) not null, -- Accidential, whole life, term, pension
	policy_name varchar(255) not null,
	number_nominees int not null,
	tenure double precision not null,
	sum_assured_min double precision not null,
	sum_assured_max double precision not null,
	pre_reqs varchar(255) not null
);
 */

public class PolicyDao {

	private final String TABLE_NAME = "Policies";
	private final String SELECT_POLICY_BY_ID = "select * from Policies where policy_id = ?";
	private final String INSERT_INTO_POLICY = "insert into " + TABLE_NAME + " values(?,?,?,?,?,?,?,?)";
	private final String SELECT_ALL_POLICY_NAME_AND_POLICY_ID = "select policy_name, policy_id from POLICIES";
	private final String SELECT_MAX_ID = "select MAX(policy_id) from " + TABLE_NAME;
	private final String UPDATE_POLICY = "UPDATE Policies " +
											"SET policy_type = ?, " +
											"policy_name = ?, " + 
											"number_nominees = ?, " +
											"tenure = ?, " +
											"sum_assured_min = ?, " +
											"sum_assured_max = ?, " +
											"pre_reqs = ? " +
											"WHERE policy_id = ? ";
	
	private final String SELECT_POLICIES_BY_CUSTOMER_ID = "SELECT * FROM PolicyMap LEFT JOIN Policies ON " + 
										"PolicyMap.policy_ID = Policies.policy_ID " + "Where PolicyMap.customer_ID = ?";

	private final String DELETE_POLICIES_BY_CUSTOMER_ID = "DELETE from policies where policy_id = ?";
  
	private final String CHECK_IF_POLICYID_IS_MAPPED = "select * from POLICYMAP where policy_id = ?";
  
	/**
	 *  Will insert a policy object into the database.
	 * @param policy - an instantiated Policy object.
	 * @return true if policy is successfully added or false if not.
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public boolean insert(Policy policy) throws SQLException, ClassNotFoundException {
			Connection con = null;
			PreparedStatement ps = null;
			
			con = OracleConnection.INSTANCE.getConnection();
		
			ps = con.prepareStatement(INSERT_INTO_POLICY);
			
			ps.setInt(1, policy.getPolicyId());
			ps.setString(2, policy.getPolicyType());
			ps.setString(3, policy.getPolicyName());
			ps.setInt(4, policy.getNumberNominees());
			ps.setDouble(5, policy.getTenure());
			ps.setDouble(6, policy.getMinSum());
			ps.setDouble(7, policy.getMaxSum());
			ps.setString(8, policy.getPreReqs());

			int rowsAffected = ps.executeUpdate();
			
			//clean up
			ps.close();
			OracleConnection.INSTANCE.disconnect();
				
			if(rowsAffected >= 1) {
				System.out.println("Policy successfully added");
				return true;
			}else {
				System.out.println("Policy was not added");
				return false;
			}
	}
	
	/**
	 *  Will retrieve a policy object.
	 * @param rs - a ResultSet object.
	 * @return a policy object containing (id, name, tenure, sumMin, sumMax, paymentsPerYear, premium)
	 * @throws SQLException
	 */
	public Policy getPolicyInformation(ResultSet rs) throws SQLException {
		Policy p = new Policy();
		p.setPolicyId(rs.getInt("policy_id"));
		p.setPolicyName(rs.getString("policy_name"));
		p.setTenure(rs.getDouble("tenure"));
		p.setMinSum(rs.getDouble("sum_assured_min"));
		p.setMaxSum(rs.getDouble("sum_assured_max"));
		p.setPaymentsPerYear(rs.getInt("payments_per_year"));
		p.setPremiumAmount(rs.getDouble("premium_amount"));
		return p;
	}
	
	public static ArrayList<Policy> getPoliciesWithType(String type) throws SQLException, ClassNotFoundException{
		ArrayList<Policy> policies = new ArrayList<Policy>();
		
		Connection con = OracleConnection.INSTANCE.getConnection();
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("Select * from Policies where Policies.policy_type='"+type+"'");
		Policy temp;
		while (rs.next()) {
			temp = new Policy();
			temp.setPolicyId(rs.getInt(1));
			temp.setPolicyType(rs.getString(2));
			temp.setPolicyName(rs.getString(3));
			temp.setNumberNominees(rs.getInt(4));
			temp.setTenure(rs.getDouble(5));
			temp.setMinSum(rs.getDouble(6));
			temp.setMaxSum(rs.getDouble(7));
			temp.setPreReqs(rs.getString(8));
			policies.add(temp);
		}
		
		rs.close();
		st.close();
		OracleConnection.INSTANCE.disconnect();
		return policies;
	}
	
	/**
	 *  Will retrieve an arraylist of policy object based on customerID
	 * @param id - an integer of customer id.
	 * @return ArrayList of Policy objects.
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public List<Policy> getPoliciesByCustomerID(int id) throws ClassNotFoundException, SQLException{
		ArrayList<Policy> policies = new ArrayList<Policy>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		con = OracleConnection.INSTANCE.getConnection();
		
		ps = con.prepareStatement(SELECT_POLICIES_BY_CUSTOMER_ID);
		ps.setInt(1, id);
		
		rs = ps.executeQuery();
		
		while(rs.next()){
			policies.add(getPolicyInformation(rs));
		}
		rs.close();
		ps.close();
		OracleConnection.INSTANCE.disconnect();
		return policies;
	}
	
	/**
	 * Will retrieve the current/max id from Policies table
	 * @return An integer holding the largest ID currently in the table.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public int getLargestID() throws SQLException, ClassNotFoundException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		con = OracleConnection.INSTANCE.getConnection();
		ps = con.prepareStatement(SELECT_MAX_ID);
		rs = ps.executeQuery();
		
		rs.next();
		int maxID = rs.getInt(1);
		
		//clean up
		rs.close();
		ps.close();
		OracleConnection.INSTANCE.disconnect();
		
		return maxID;
	}
	
	/**
	 * Added by Domenic Garreffa on Aug 16, 2018
	 * 
	 * Updates existing Policy where ID matches method paramter.
	 * @param policy
	 * @return True if Policy table was affected. IE. Policy was altered succesfully and false otherwise.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public boolean update(Policy policy, int policyID) throws SQLException, ClassNotFoundException {
		Connection con = null;
		PreparedStatement ps = null;
		
		con = OracleConnection.INSTANCE.getConnection();
		
		ps = con.prepareStatement(UPDATE_POLICY);
		
		ps.setString(1, policy.getPolicyType());
		ps.setString(2, policy.getPolicyName());
		ps.setInt(3, policy.getNumberNominees());
		ps.setDouble(4, policy.getTenure());
		ps.setDouble(5, policy.getMinSum());
		ps.setDouble(6, policy.getMaxSum());
		ps.setString(7, policy.getPreReqs());
		ps.setInt(8, policyID);

		int rowsAffected = ps.executeUpdate();
		
		//clean up
		ps.close();
		con.close();
			
		if(rowsAffected >= 1) {
			System.out.println("Policy successfully updated");
			return true;
		}else {
			System.out.println("Policy was not updated.");
			return false;
		}
	}
	
	/**
	 * Method to return a List of Policies. The list will simply contain every policy
	 * in the database. 
	 * 
	 * Created by Nicholas Kauldhar on August 16 around 2pm
	 * Updated by Nicholas Kauldhar August 17 around 9am since schema change
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static List<Policy> getAllPolicies () throws SQLException, ClassNotFoundException {
		Connection con = OracleConnection.INSTANCE.getConnection();
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("Select * from Policies");
		
		List<Policy> k = new ArrayList<Policy>();
		
		Policy temp;
		while (rs.next()) {
			temp = new Policy();
			temp.setPolicyId(rs.getInt(1));
			temp.setPolicyType(rs.getString(2));
			temp.setPolicyName(rs.getString(3));
			temp.setNumberNominees(rs.getInt(4));
			temp.setTenure(rs.getDouble(5));
			temp.setMinSum(rs.getDouble(6));
			temp.setMaxSum(rs.getDouble(7));
			temp.setPreReqs(rs.getString(8));
			k.add(temp);
		}
		
		rs.close();
		st.close();
		OracleConnection.INSTANCE.disconnect();
		
		return k;
	}
	
	/**
	 * Method to return a List of Policies. The list will simply contain every policy
	 * in the database. 
	 * 
	 * Created by Nicholas Kauldhar on August 16 around 2pm
	 * Updated by Nicholas Kauldhar August 17 around 9am since schema change
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static List<Policy> getAllCustomerPolicies (int id) throws SQLException, ClassNotFoundException {
		Connection con = OracleConnection.INSTANCE.getConnection();
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("Select * from PolicyMap where CUSTOMER_ID = " + id);
		List<Integer> policyIDs = new ArrayList<Integer>();
		List<List<Nominee>> nomineeLists = new ArrayList<List<Nominee>>();
		List<Date> dates = new ArrayList<Date>();
		List<Integer> payments = new ArrayList<Integer>();
		List<Double> premiums = new ArrayList<Double>();

		while (rs.next()) {
			policyIDs.add(rs.getInt(3));
			nomineeLists.add(NomineeDao.getNomineesFromMapID(rs.getInt(1)));
			dates.add(rs.getDate(5));
			payments.add(rs.getInt(6));
			premiums.add(rs.getDouble(7));
			
			
		}
		
		List<Policy> k = new ArrayList<Policy>();
		PreparedStatement pst = con.prepareStatement("Select * from Policies where POLICY_ID = ?");
		Policy temp;
		rs.close();
		
		int index = 0;
		
		for (int x = 0; x < policyIDs.size(); x++) {
			pst.setInt(1, policyIDs.get(x));
			rs = pst.executeQuery();
			if (rs.next()) {
				temp = new Policy();
				temp.setPolicyId(rs.getInt(1));
				temp.setPolicyType(rs.getString(2));
				temp.setPolicyName(rs.getString(3));
				temp.setNumberNominees(rs.getInt(4));
				temp.setTenure(rs.getDouble(5));
				temp.setMinSum(rs.getDouble(6));
				temp.setMaxSum(rs.getDouble(7));
				temp.setPreReqs(rs.getString(8));
				temp.setNominees(nomineeLists.get(index));
				temp.setPaymentsPerYear(payments.get(index));
				temp.setPremiumAmount(premiums.get(index));
				temp.setStartDate(dates.get(index));
				k.add(temp);
				index++;
			}
		}
		
		rs.close();
		pst.close();
		st.close();
		OracleConnection.INSTANCE.disconnect();
		
		return k;
	}


	/**
	 * Method used by Admin to generate certificates. It uses customer and 
	 * policy ID to find a PolicyMap. It then stores the ID of that policy map
	 * in the session object to be further used in other methods. Returns true if
	 * a PolicyMap is returned and false otherwise.
	 * 
	 * Created by Nicholas Kauldhar on August 16 around 3pm
	 * 
	 * @param request
	 * @return boolean
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static boolean searchByCustandPolicy(HttpServletRequest request) throws SQLException, ClassNotFoundException {
		String custid = (String)request.getSession().getAttribute("customerID");
		String polid = request.getParameter("policyID");
		
		int c = -1;
		int d = -1;
		try {
			c = Integer.parseInt(custid);
			d = Integer.parseInt(polid);
		}
		catch(Exception e) {
			return false;
		}
		
		try{
			Connection con = OracleConnection.INSTANCE.getConnection();
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery("Select * from PolicyMap where customer_id = " 
			+ c + " and policy_id = " + d);
			
			if(rs.next()) {
				PolicyDao dao = new PolicyDao();
				Date date = rs.getDate(5);
				Policy pol = dao.selectPolicyByID(d);
				//calculate expiry date based on tenure
				LocalDate ld = LocalDate.parse(date.toString());
				long days = (long)(pol.getTenure()*365.0);
				ld = ld.plusDays(days);
				
				date = Date.valueOf(ld);
				List<Nominee> noms = NomineeDao.getNomineesFromMapID(rs.getInt(1));
				
				request.getSession().setAttribute("CertPremium", rs.getDouble(7));
				request.getSession().setAttribute("CertNominees", noms);				
				request.getSession().setAttribute("CertEndDate", date);
				request.getSession().setAttribute("CertPolicy", pol);
				
				rs.close();
				st.close();
				return true;
			}
			else {
				return false;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			OracleConnection.INSTANCE.disconnect();
		}
	}
	
	/**
	 * Method retrieves the id and name information of all policies in the policues table
	 * and format the information into a string.
	 * 
	 * Created by Nicholas Kauldhar on August 16 around 3pm
	 * 
	 * @return an arraylist of strings, which represents policy names and ids
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public ArrayList<String> selectAllPolicyNameAndPolicyID() throws ClassNotFoundException, SQLException{
 		Connection con = null;
 		PreparedStatement ps = null;
 		ResultSet rs = null;
 		
 		con = OracleConnection.INSTANCE.getConnection();
 		ps = con.prepareStatement(SELECT_ALL_POLICY_NAME_AND_POLICY_ID);
 		rs = ps.executeQuery();
 		
 		ArrayList<String> policyNameAndIDConcatList = new ArrayList<>();
 		
 		while(rs.next()) {
 			policyNameAndIDConcatList.add(rs.getString(1) + '(' + rs.getString((2)) + ')');
 		}
 		
 		//clean up
 		ps.close();
 		con.close();
 		rs.close();
 			
 		if(!policyNameAndIDConcatList.isEmpty()) {
 			System.out.println("Policies successfully retrieved.");
 			return policyNameAndIDConcatList;
 		}else {
 			System.out.println("No policies retrieved.");
 			return null;
 		}
 	}
 	
 	/**
 	 * Get a policy object given an ID
 	 * @param ID
 	 * @return
 	 * @throws ClassNotFoundException
 	 * @throws SQLException
 	 */
 	public Policy selectPolicyByID(int ID) throws ClassNotFoundException, SQLException{
 		Connection con = null;
 		PreparedStatement ps = null;
 		ResultSet rs = null;
 		
 		con = OracleConnection.INSTANCE.getConnection();
 		ps = con.prepareStatement(SELECT_POLICY_BY_ID);		
 		ps.setInt(1, ID);
 		rs = ps.executeQuery();

 		Policy policy = null;
 		
 		if(rs.next()) {
	 		policy = new Policy();
	 		policy.setPolicyId(rs.getInt(1));
	 		policy.setPolicyType(rs.getString(2));
	 		policy.setPolicyName(rs.getString(3));
	 		policy.setNumberNominees(rs.getInt(4));
	 		policy.setTenure(rs.getDouble(5));
	 		policy.setMinSum(rs.getDouble(6));
	 		policy.setMaxSum(rs.getDouble(7));
	 		policy.setPreReqs(rs.getString(8));	
 		}

 		//clean up
 		rs.close();
 		ps.close();
 		con.close();
 			
 		if(policy != null) {
 			System.out.println("Policy found.");
 			return policy;
 		}else {
 			System.out.println("No policies found.");
 			return null;
 		}
 	}
 	
 	/*
 	 *Created by Hamza. This function is used to access the database and check whether a customer is 
 	 *mapped to a policy.
 	 *returns a boolean
 	 */
 	
 	
 	public boolean checkPolicyMapWithPolicyId(int ID) throws ClassNotFoundException, SQLException {
 		boolean check = false;
 		
		Connection con = null;
 		PreparedStatement ps = null;
 		ResultSet rs = null;
 		
 		con = OracleConnection.INSTANCE.getConnection();
 		ps = con.prepareStatement(CHECK_IF_POLICYID_IS_MAPPED);		
 		ps.setInt(1, ID);
 		rs = ps.executeQuery();
 		
 		if(rs.next()) {
 			check=true;
 		}
 		
 		//clean up
 		rs.close();
 		ps.close();
 		con.close();
 			
 		return check;
 		}
 	
 	public boolean deletePolicyUsingId(int ID) throws ClassNotFoundException, SQLException {
 		
 		Connection con = null;
 		PreparedStatement ps = null;
 		ResultSet rs = null;
 		
 		con = OracleConnection.INSTANCE.getConnection();
 		ps = con.prepareStatement(DELETE_POLICIES_BY_CUSTOMER_ID);
 		ps.setInt(1, ID);
 		rs = ps.executeQuery();
 		
 		rs.close();
 		ps.close();
 		con.close();
 
 		return true;
 	}
 	
}