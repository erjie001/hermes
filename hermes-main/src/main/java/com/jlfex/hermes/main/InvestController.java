package com.jlfex.hermes.main;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.jlfex.hermes.common.App;
import com.jlfex.hermes.common.AppUser;
import com.jlfex.hermes.common.Logger;
import com.jlfex.hermes.common.Result;
import com.jlfex.hermes.common.Result.Type;
import com.jlfex.hermes.common.cache.Caches;
import com.jlfex.hermes.common.utils.Calendars;
import com.jlfex.hermes.common.utils.Strings;
import com.jlfex.hermes.model.Dictionary;
import com.jlfex.hermes.model.Invest;
import com.jlfex.hermes.model.InvestProfit;
import com.jlfex.hermes.model.Loan;
import com.jlfex.hermes.model.LoanAuth;
import com.jlfex.hermes.model.LoanLog;
import com.jlfex.hermes.model.Repay;
import com.jlfex.hermes.model.User;
import com.jlfex.hermes.model.UserAccount;
import com.jlfex.hermes.service.DictionaryService;
import com.jlfex.hermes.service.InvestProfitService;
import com.jlfex.hermes.service.InvestService;
import com.jlfex.hermes.service.LabelService;
import com.jlfex.hermes.service.LoanService;
import com.jlfex.hermes.service.ProductService;
import com.jlfex.hermes.service.PropertiesService;
import com.jlfex.hermes.service.RepayService;
import com.jlfex.hermes.service.UserInfoService;
import com.jlfex.hermes.service.pojo.InvestInfo;
import com.jlfex.hermes.service.pojo.LoanUserInfo;

/**
 * @author chenqi
 * @version 1.0, 2013-12-23
 * @since 1.0
 * 
 */
@Controller
@RequestMapping("/invest")
public class InvestController {

	/** 理财业务接口 */
	@Autowired
	private LoanService loanService;

	@Autowired
	private ProductService productService;

	@Autowired
	private InvestService investService;

	@Autowired
	private InvestProfitService investProfitService;

	@Autowired
	private UserInfoService userInfoService;

	/** 系统属性业务接口 */
	@Autowired
	private PropertiesService propertiesService;

	@Autowired
	private RepayService repayService;
	@Autowired
	private DictionaryService dictionaryService;

	@Autowired
	private LabelService labelService;

	// 正在招标中的Cache的info
	private static final String CACHE_LOAN_DEADLINE_PREFIX = "com.jlfex.hermes.cache.loan.deadline.";
	private static final String INVEST_BID_MULTIPLE = "invest.bid.multiple";

	@RequestMapping("checkMoneyMore")
	@ResponseBody
	public JSONObject checkMoneyMore(BigDecimal investamount, String loanid) {

		Logger.info("investamount:" + investamount + "loanid:" + loanid);
		Loan loan = loanService.loadById(loanid);
		BigDecimal remain = loan.getAmount().subtract(loan.getProceeds());
		Logger.info("Remain:" + remain);
		JSONObject jsonObj = new JSONObject();
		// 大于返回false提示不成功信息
		if (investamount.compareTo(remain) == 1) {
			jsonObj.put("investamount", false);
		} else {
			jsonObj.put("investamount", true);
		}
		return jsonObj;
	}

	@RequestMapping("checkMoneyLess")
	@ResponseBody
	public JSONObject checkMoneyLess(BigDecimal investamount) {

		Logger.info("investamount:" + investamount);
		AppUser curUser = App.current().getUser();
		UserAccount userAccount = userInfoService.loadByUserIdAndType(curUser.getId(), UserAccount.Type.CASH);
		BigDecimal balance = userAccount.getBalance();
		Logger.info("balance:" + balance);
		JSONObject jsonObj = new JSONObject();
		// 大于返回false提示不成功信息
		if (investamount.compareTo(balance) == 1) {
			jsonObj.put("investamount", false);
		} else {
			jsonObj.put("investamount", true);
		}
		return jsonObj;
	}

	/**
	 * @param mode
	 * @return
	 */
	@RequestMapping("/display")
	public String display(Model model) {
		App.checkUser();
		List<Dictionary> loanPurposeList = dictionaryService.findByTypeCode("loan_purpose");
		model.addAttribute("loanpurposes", loanPurposeList);
		List<Repay> repayList = repayService.findAll();
		model.addAttribute("repays", repayList);
		model.addAttribute("nav", "invest");
		return "invest/display";
	}

	/**
	 * 索引
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/index")
	public String index(Model model) {
		model.addAttribute("purposes", dictionaryService.findByTypeCode("loan_purpose"));
		model.addAttribute("repays", repayService.findAll());
		model.addAttribute("nav", IndexController.HomeNav.INVEST);
		return "invest/index";
	}

	//
	// /**
	// * 和服务器时间相减，计算截至日期
	// *
	// * @param request
	// * @param model
	// * @return
	// */
	// @RequestMapping("/caldeadline")
	// @ResponseBody
	// public String caldeadline(String deadline, Model model) {
	// try {
	// if (!Strings.blank(deadline)) {
	// int day = 24 * 60 * 60 * 1000;
	// int hour = 60 * 60 * 1000;
	// int minute = 60 * 1000;
	// int second = 1000;
	// Date end = Calendars.parseDateTime(deadline);
	// Date start = new Date();
	// long endTime = end.getTime();
	// long startTime = start.getTime();
	// if (endTime - startTime > 0) {
	// long days = (endTime - startTime) / day;// 化为天
	// long hours = (endTime - startTime) % day / hour;// 化为时
	// long minutes = (endTime - startTime) % day % hour / minute;// 化为分
	// long seconds = (endTime - startTime) % day % hour % minute / second;//
	// 化为分
	// return String.valueOf(days) + "天" + String.valueOf(hours) + "时" +
	// String.valueOf(minutes) + "分" + String.valueOf(seconds) + "秒";
	// }
	// }
	// return "";
	// } catch (Exception e) {
	// return "";
	// }
	// }

	/**
	 * 投标操作
	 * 
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping("/bid")
	@ResponseBody
	public Result bid(String loanid, String investamount, String otherrepayselect) {
		Result result = new Result();
		try {
			App.checkUser();
		} catch (Exception ex) {
			result.setType(Type.WARNING);
			return result;
		}

		AppUser curUser = App.current().getUser();
		User user = userInfoService.findByUserId(curUser.getId());
		Logger.info("loanid:" + loanid + ",investamount:" + investamount + ",otherrepayselect :" + otherrepayselect);
		boolean bidResult = investService.bid(loanid, user, new BigDecimal(investamount), otherrepayselect);
		if (bidResult) {
			result.setType(Type.SUCCESS);
		} else {
			result.setType(Type.FAILURE);
		}
		return result;

	}

	@RequestMapping("/bidsuccess")
	public String bidsuccess(String investamount, String loanid, Model model) {
		App.checkUser();
		model.addAttribute("nav", "invest");
		model.addAttribute("investamount", investamount);
		model.addAttribute("loanid", loanid);

		// 返回视图
		return "invest/bidsuccess";
	}

	@RequestMapping("/bidfull")
	public String bidfull(Model model) {
		App.checkUser();
		model.addAttribute("nav", "invest");
		// 返回视图
		return "invest/bidfull";
	}

	/**
	 * 计算预期收益
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping("/calmaturegain")
	@ResponseBody
	public BigDecimal calmaturegain(HttpServletRequest request) {
		String loanid = request.getParameter("loanid");
		String investamount = request.getParameter("investamount");
		Logger.info("loanid:" + loanid + "investamount:" + investamount);
		Loan loan = loanService.loadById(loanid);
		BigDecimal maturegain = repayService.getRepayMethod(loan.getRepay().getId()).getProceeds(loan, null, new BigDecimal(investamount));

		return maturegain;
	}

	/**
	 * 借款列表查询
	 * 
	 * @param purpose
	 * @param raterange
	 * @param periodrange
	 * @param repayname
	 * @param page
	 * @param size
	 * @param orderByField
	 * @param orderByDirection
	 * @param model
	 * @return
	 */
	@RequestMapping("/indexsearch")
	public String indexsearch(String purpose, String raterange, String periodrange, String repayname, String page, String size, String orderByField, String orderByDirection, Model model) {
		Logger.info("indexsearch purpose" + getUTFFormat(purpose) + "raterange" + getUTFFormat(raterange) + "periodrange" + getUTFFormat(periodrange) + "repayname" + getUTFFormat(repayname));
		model.addAttribute("loans",
				investService.findByJointSql(getUTFFormat(purpose), getUTFFormat(raterange), getUTFFormat(periodrange), getUTFFormat(repayname), page, size, orderByField, orderByDirection));
		Logger.info("indexsearch end");
		return "invest/loandata";
	}

	/**
	 * 把乱码解析成中文
	 * 
	 * @param src
	 * @return
	 */
	private String getUTFFormat(String src) {
		if (!Strings.empty(src)) {
			String dest = "";
			try {
				dest = new String(src.getBytes("iso-8859-1"), "utf-8");
			} catch (UnsupportedEncodingException e) {
				Logger.error("getUTFFormat Message Error:" + e.getMessage());
			}
			return dest;
		} else
			return "";
	}

	/**
	 * 借款明细
	 * 
	 * @param id
	 * @param model
	 * @return
	 */
	/**
	 * @param model
	 * @param loanid
	 * @return
	 */
	@RequestMapping("/info")
	public String info(Model model, String loanid) {
		App.checkUser();
		Logger.info("loanid:" + loanid);
		Loan loan = loanService.loadById(loanid);
		model.addAttribute("loan", loan);
		Dictionary dictionary = dictionaryService.loadById(loan.getPurpose());
		model.addAttribute("purpose", dictionary.getName());
		model.addAttribute("product", loan.getProduct());
		model.addAttribute("repay", loan.getProduct().getRepay());
		model.addAttribute("user", loan.getUser());
		// 从借款日志表里取开始投标的起始时间

		if (Caches.get(CACHE_LOAN_DEADLINE_PREFIX + loanid) == null) {
			LoanLog loanLogStartInvest = loanService.loadLogByLoanIdAndType(loanid, LoanLog.Type.START_INVEST);
			if (loanLogStartInvest != null && loanLogStartInvest.getDatetime() != null) {
				String duration = String.valueOf(loan.getDeadline()) + "d";
				Date datetimeloanLogStartInvest = loanLogStartInvest.getDatetime();
				Date deadline = Calendars.add(datetimeloanLogStartInvest, duration);
				Caches.set(CACHE_LOAN_DEADLINE_PREFIX + loanid, deadline, "7d");
			}
		}
		if (Caches.get(CACHE_LOAN_DEADLINE_PREFIX + loanid) != null) {
			Date deadline = Caches.get(CACHE_LOAN_DEADLINE_PREFIX + loanid, Date.class);
			Date start = new Date();
			long endTime = deadline.getTime();
			long startTime = start.getTime();
			if (endTime - startTime > 0) {
				model.addAttribute("remaintime", String.valueOf(endTime - startTime));
			} else {
				model.addAttribute("remaintime", 0);
			}
			// 投标最后期限
		} else {
			model.addAttribute("remaintime", 0);
		}
		LoanUserInfo loanUserInfo = loanService.loadLoanUserInfoByUserId(App.user().getId());
		model.addAttribute("loanUserInfo", loanUserInfo);

		List<Invest> investList = investService.findByLoan(loan);
		model.addAttribute("invests", investList);

		List<LoanAuth> loanAuthlist = loanService.findLoanAuthByLoan(loan);

		model.addAttribute("loanauths", loanAuthlist);

		model.addAttribute("nav", "invest");
		//读取投标金额倍数设置
		String investBidMultiple = App.config(INVEST_BID_MULTIPLE);
		model.addAttribute("investBidMultiple", investBidMultiple);
		// 返回视图
		return "invest/info";
	}

	/**
	 * 我的理财
	 * 
	 * @param userid
	 * @param model
	 * @return
	 */
	@RequestMapping("/myinvest")
	public String myinvest(Model model) {
		App.checkUser();
		AppUser curUser = App.current().getUser();

		User user = userInfoService.findByUserId(curUser.getId());
		// 已获收益
		BigDecimal allProfitSum = investProfitService.loadSumAllProfitByUserAndInStatus(user, new String[] { InvestProfit.Status.ALREADY, InvestProfit.Status.OVERDUE, InvestProfit.Status.ADVANCE });
		// 利息
		BigDecimal interestSum = investProfitService.loadInterestSumByUserAndInStatus(user, new String[] { InvestProfit.Status.ALREADY, InvestProfit.Status.OVERDUE, InvestProfit.Status.ADVANCE });
		// 罚息
		BigDecimal overdueInterestSum = investProfitService.loadOverdueInterestSumByUserAndInStatus(user, new String[] { InvestProfit.Status.ALREADY, InvestProfit.Status.OVERDUE,
				InvestProfit.Status.ADVANCE });

		List<InvestInfo> investInfoList = investService.findByUser(user);
		int investSuccessCount = 0;
		for (InvestInfo investInfo : investInfoList) {
			if (Invest.Status.COMPLETE.equals(investInfo.getStatus())) {
				investSuccessCount = investSuccessCount + 1;
			}

		}
		if (allProfitSum == null)
			allProfitSum = BigDecimal.ZERO;
		if (interestSum == null)
			interestSum = BigDecimal.ZERO;
		if (overdueInterestSum == null)
			overdueInterestSum = BigDecimal.ZERO;
		model.addAttribute("allProfitSum", allProfitSum.setScale(2, BigDecimal.ROUND_UP));
		model.addAttribute("interestSum", interestSum.setScale(2, BigDecimal.ROUND_UP));
		model.addAttribute("overdueInterestSum", overdueInterestSum.setScale(2, BigDecimal.ROUND_UP));
		model.addAttribute("successCount", investSuccessCount);

		model.addAttribute("invests", investInfoList);
		model.addAttribute("nav", "invest");
		// 返回视图
		return "invest/myinvest";
	}

	/**
	 * 我的理财的明细
	 * 
	 * @param investid
	 * @param model
	 * @return
	 */
	@RequestMapping("/myinvestinfo/{invest}")
	public String myinvestinfo(@PathVariable("invest") String investid, HttpServletRequest request, Model model) {
		App.checkUser();
		Invest invest = investService.loadById(investid);
		// String page = request.getParameter("page");
		// String size = request.getParameter("size");
		Loan loan = invest.getLoan();
		model.addAttribute("loan", loan);
		Dictionary dictionary = dictionaryService.loadById(loan.getPurpose());
		model.addAttribute("product", loan.getProduct());
		model.addAttribute("purpose", dictionary.getName());
		model.addAttribute("repay", loan.getProduct().getRepay());
		model.addAttribute("user", loan.getUser());
		model.addAttribute("investprofitinfos", investProfitService.getInvestProfitRecords(invest));
		model.addAttribute("nav", "invest");
		// 返回视图
		return "invest/myinvestinfo";
	}
}
