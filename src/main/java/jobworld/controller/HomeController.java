package jobworld.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.validation.ConstraintViolationException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import jobworld.model.entities.JobOffer;
import jobworld.model.entities.Person;
import jobworld.model.entities.User;
import jobworld.model.entities.Role.TypeRole;
import jobworld.services.CompanyService;
import jobworld.services.JobOfferService;
import jobworld.services.PersonService;
import jobworld.services.RoleService;
import jobworld.services.UserService;

@Controller
public class HomeController {

	/**
	 * Classe Controllore Home
	 * 
	 * @author Giuseppe Costantini
	 * @author Simone di Saverio
	 * @author Lorenzo Giuliani
	 * @author Savio Feng
	 * @version 1.0
	 */
	private JobOfferService jobOfferService;
	private UserService userService;
	private PersonService personService;
	private CompanyService companyService;
	private RoleService roleService;
	public static final Pattern valid_email = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
			Pattern.CASE_INSENSITIVE);
	
	@ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
	public String handleException(org.springframework.web.client.ResourceAccessException ex){
	    return "/JobWorld?error=true";
	}

	@GetMapping
	public String home(@RequestParam(value="error", defaultValue = "", required = false) String error, Locale locale, Model model) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth.getName() != "anonymousUser" && auth.getAuthorities().toString().equals("[ROLE_USER]")) {
			Person person = personService.findbyUserId(auth.getName()); //ci prendiamo la persona attuale
			model.addAttribute("person", person);
		}
		List<JobOffer> allJobOffers = this.jobOfferService.findAll();
		List<String> company_image = new ArrayList<String>();
		for (JobOffer job : allJobOffers) {
			company_image.add(job.getCompany().getUser().getImage());
		}
		model.addAttribute("jobOffers", allJobOffers);
		model.addAttribute("image", company_image);

		// Implementazione delle api rest per ip address in base alla zona di
		// appartenenza;
		// TODO: cambiate l'ip per vedere come la form filter cambia automaticamente i
		// nomi di regione, citt�, provincia.
		// String ip ="79.18.192.39"; //Abruzzo Atri
		String ip = "37.160.70.194"; // Lazio Roma
		// String ip ="2.235.168.0"; // Nichelino Piemonte
		String uri = "https://ipapi.co/" + ip + "/json/";
		try {
			RestTemplate restTemplate = new RestTemplate();
			String result = restTemplate.getForObject(uri, String.class);
			JSONObject obj = new JSONObject(result);
			model.addAttribute("region", obj.getString("region"));
			model.addAttribute("city", obj.getString("city"));
		} catch (org.springframework.web.client.ResourceAccessException ex) {
			return "home";
		}

		// Per estrarre ip dal client che effettua la richiesta
		/*
		 * String ip_client=request.getRemoteAddr(); if (ip_client== null) { ip_client =
		 * request.getHeader("X-FORWARDED-FOR"); // Nel caso di collegamento attraverso
		 * proxy serve comunque a trovare un ip }
		 */

		/*
		 * List<Company> companys = companyService.findAll(); Collections.sort(companys,
		 * (a,b)-> a.getJobOffers().size() < b.getJobOffers().size() ? -1 :
		 * a.getJobOffers().size() == b.getJobOffers().size() ? 0 : 1);
		 * model.addAttribute("companys", companys);
		 */
		return "home";
	}

	@Autowired
	public void setJobOfferService(JobOfferService jobOfferService) {
		this.jobOfferService = jobOfferService;
	}

	@Autowired
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	@Autowired
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	@Autowired
	public void setCompanyService(CompanyService companyService) {
		this.companyService = companyService;
	}

	@Autowired
	public void setRoleService(RoleService roleService) {
		this.roleService = roleService;
	}

	@GetMapping("/moreinfo/{companyid}/{jobid}")
	public String moreinfo(@PathVariable(value = "jobid") Long jobId, @PathVariable(value = "companyid") Long companyId,
			Model model) {
		JobOffer joboffer = jobOfferService.findbyId(jobId); // ci prendiamo la joboffer tramite l'id
		model.addAttribute("joboffer", joboffer);
		return "moreinfo";
	}

	@PostMapping(value = "/filter")
	public String filter(@RequestParam Map<String, String> allParams, Model model) {
		List<JobOffer> jobOffers = this.jobOfferService.filter(allParams.get("region"), allParams.get("province"),
				allParams.get("town"), allParams.get("position"), allParams.get("contractType"),
				allParams.get("minEducationLevel"), allParams.get("minExperience")); 
		model.addAttribute("jobOffers", jobOffers); //restituisco al model la lista delle offerte filtrate
		return "home";
	}

	@GetMapping("/register")
	public String register(@RequestParam(value = "date", defaultValue = "", required = false) String date_error,
			@RequestParam(value = "error", defaultValue = "", required = false) String error,
			@RequestParam(value = "existing", defaultValue = "", required = false) String existing,
			@RequestParam(value = "con", defaultValue = "", required = false) String con, Model model) {
		model.addAttribute("date_error", date_error);
		model.addAttribute("existing", existing);
		model.addAttribute("error", error);
		model.addAttribute("con", con);
		return "register";
	}

	@PostMapping("/add")
	public String add(@RequestParam Map<String, String> allParams) {
		if (!valid_email.matcher(allParams.get("email")).find()) {
			return "redirect:/register?error=true";
		}
		try {
			LocalDate birthDate = null;
			if (allParams.containsKey("birthDate")) {
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					birthDate = LocalDate.parse(allParams.get("birthDate"), formatter);
				} catch (DateTimeParseException e) {
					return "redirect:/register?date=true"; //errore formattazione data
				}
			}
			User user = userService.create(allParams.get("email"),
					userService.encryptPassword(allParams.get("password")), null, null);
			if (allParams.get("type").equals("person")) {
				user.addRole(roleService.getRoleByTypeRole(TypeRole.USER));
				try {
					personService.create(allParams.get("firstName"), allParams.get("secondName"), birthDate,
							allParams.get("number"), null, user);
				} catch (ConstraintViolationException e) {
					user.setPerson(null); //devo settare person null e canc user dal db perch� altrimenti
					//resta inserito l'user non valido
					user=userService.update(user);
					userService.delete(user);
					return "redirect:/register?con=true"; // violata costraint di qualche dato obbligatorio non inserito
				}
			} else if (allParams.get("type").equals("company")) {
				user.addRole(roleService.getRoleByTypeRole(TypeRole.COMPANY));
				try {
					companyService.create(allParams.get("name"), user);
				} catch (ConstraintViolationException e) {
					user.setCompany(null); //idem a person
					user=userService.update(user);
					userService.delete(user);
					return "redirect:/register?con=true"; // violata costraint di qualche dato obbligatorio non inserito
				}
			}

		} catch (DataIntegrityViolationException e) {
			return "redirect:/register?existing=true"; //l'email gi� esiste
		}
		return "redirect:/login";

	}

	@GetMapping("/login")
	public String login(@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout, Model model) {
		String errorMessage = null;
		if (error != null) {
			errorMessage = "Username o Password errati!!";
		}
		if (logout != null) {
			// entriamo in questo caso se non specifichiamo una .logoutSuccessUrl in
			// WebSecurityConf.configure
			errorMessage = "Uscita dal sistema avvenuta!!";
		}
		model.addAttribute("errorMessage", errorMessage);
		return "login";
	}

	@GetMapping("/chisiamo")
	public String chisiamo() {
		return "chisiamo";
	}

	@GetMapping("/faq")
	public String faq() {
		return "faq";
	}
}