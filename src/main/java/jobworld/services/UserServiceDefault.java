package jobworld.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import jobworld.model.dao.UserDao;
import jobworld.model.entities.Role;
import jobworld.model.entities.User;
@Transactional
@Service("userService")
public class UserServiceDefault implements UserService,UserDetailsService {
	
	private UserDao userRepository;
	
	/**
	 * Metodo ovverride loadUserByUsername
	 * @param email � l'email
	 * 
	 * @return restituisce un eccezione se non trova l'user altrimenti builda il ruolo con l'utente
	 */
	@Transactional(readOnly = true)
	  @Override
	  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

	    User user = userRepository.findByEmail(email);
	    UserBuilder builder = null;
	    if (user != null) {
	      
	      // qui "mappiamo" uno User della nostra app in uno User di spring
	      builder = org.springframework.security.core.userdetails.User.withUsername(email); //popola attributo username dello userbuilder
	      builder.password(user.getPassword()); //popola attributo password dello userbuilder
	            
	      String [] roles = new String[user.getRoles().size()];

	      int j = 0;
	      for (Role r : user.getRoles()) {
	    	  roles[j++] = r.getName().toString();
	      }
	      System.out.print(roles);
	      builder.roles(roles); //popola i ruoli delo userbuilder
	    } else {
	      throw new UsernameNotFoundException("User not found.");
	    }
	    return builder.build(); //restituisce l'user di Spring
	  }
	
	/**
	 * Metodo che implementa la create
	 * @param email  email
	 * @param password  password
	 * @param description descrizione
	 * @param image immagine
	 * 
	 * @return restituisce il repository aggiornato con il nuovo utente
	 */
	@Transactional
	@Override
	public User create(String email, String password, String description, String image) {
		return this.userRepository.create(email, password, description, image);
	}
	/**
	 * Metodo che implementa l'update
	 * @param user � l'utente
	 * 
	 * @return restituisce il repository aggiornato
	 */
	@Override
	public User update(User user) {
		return this.userRepository.update(user);
	}
	/**
	 * Metodo che implementa la delete
	 * @param user � l'utente
	 * 
	 * @return restituisce il repository aggiornato dopo la delete
	 */
	@Override
	public void delete(User user) {
		user.getRoles().clear();
		user=this.userRepository.update(user); //non serve fare update di Role in quanto relazione asimmetrica e monodirezionale
		this.userRepository.delete(user);
	}
	/**
	 * Metodo che implementa findByEmail
	 * @param email email
	 * 
	 * @return restituisce l'utente tramite l'email
	 */
	@Override
	public User findByEmail(String email) {
		return this.userRepository.findByEmail(email);
	}
	/**
	 * Metodo che implementa findAll
	 * 
	 * 
	 * @return restituisce tutti gli utenti
	 */
	@Override
	public List<User> findAll() {
		return this.userRepository.findAll();
	}
	
	/**
	 * Metodi getters e setters
	 * 
	 * 
	 * 
	 */
	@Autowired
	public void setUserRepository(UserDao userRepository) {
		this.userRepository = userRepository;
	}
	/**
	 * Metodo che implementa findByMailandPassword
	 * @param email email
	 * @param password password
	 * 
	 * @return restituisce l'utente tramite email e password
	 */
	 @Override public User findByMailandPassword(String email, String password) {
	 return this.userRepository.findByMailandPassword(email, password); }
	 

	/**
	 * Metodo che implementa encryptPassword
	 * @param password password
	 * 
	 * @return restituisce la cifratura delle password
	 */
	@Override
	public String encryptPassword(String password) {
		return this.userRepository.encryptPassword(password);
	}

}
