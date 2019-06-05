/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package de.ecm4u.alfresco.ldapmt.security.authentication.ldapmt;

import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.security.authentication.AbstractAuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationContext;
import org.alfresco.repo.security.authentication.AuthenticationDiagnostic;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.authentication.ldap.LDAPInitialDirContextFactory;
import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.alfresco.repo.security.sync.ldap.LDAPNameResolver;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Authenticates a user by LDAP. To convert the user name to an LDAP DN, it uses the fixed format in
 * <code>userNameFormat</code> if set, or calls the {@link LDAPNameResolver} otherwise.
 * 
 * @author Andy Hind
 */
public class LDAPAuthenticationComponentImpl extends AbstractAuthenticationComponent implements InitializingBean,
        ActivateableBean
{
	    
    // ldapmt PATCH START
    private final Log logger = LogFactory.getLog(getClass());

    // Since these services are private in the extended class and we want to use
    // them in the pachted code, we must make them members here.
    private UserRegistrySynchronizer userRegistrySynchronizer;    
    private AuthenticationContext authenticationContext;
    private TransactionService transactionService;
    private PersonService personService;
    // ldapmt PATCH END
	
    private boolean escapeCommasInBind = false;
    
    private boolean escapeCommasInUid = false;
    
    private boolean active = true;

    private String userNameFormat;
    
    private LDAPNameResolver ldapNameResolver;

    private LDAPInitialDirContextFactory ldapInitialContextFactory;

    public LDAPAuthenticationComponentImpl()
    {
        super();
    }

    public void setLDAPInitialDirContextFactory(LDAPInitialDirContextFactory ldapInitialDirContextFactory)
    {
        this.ldapInitialContextFactory = ldapInitialDirContextFactory;
    }

    public void setUserNameFormat(String userNameFormat)
    {
        this.userNameFormat = userNameFormat == null || userNameFormat.length() == 0 ? null : userNameFormat;
    }
        
    public void setLdapNameResolver(LDAPNameResolver ldapNameResolver)
    {
        this.ldapNameResolver = ldapNameResolver;
    }

    public void setEscapeCommasInBind(boolean escapeCommasInBind)
    {
        this.escapeCommasInBind = escapeCommasInBind;
    }

    public void setEscapeCommasInUid(boolean escapeCommasInUid)
    {
        this.escapeCommasInUid = escapeCommasInUid;
    }
    
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.ActivateableBean#isActive()
     */
    public boolean isActive()
    {
        return this.active;
    }        

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception
    {
        if (this.ldapNameResolver == null && this.userNameFormat == null)
        {
            throw new IllegalStateException("At least one of ldapNameResolver and userNameFormat must be set");
        }
    }

    /**
     * Implement the authentication method
     */
    protected void authenticateImpl(String userName, char[] password) throws AuthenticationException
    {       
        // ldapmt PATCH START
        if (logger.isDebugEnabled()) {
            logger.debug("userName=" + userName);
        }
        if (!userName.contains("@")) {
            throw new AuthenticationException("no tenant in " + userName);
        }

        String tenant;
        String[] userNameParts = userName.split("@");
        if (userNameParts.length == 3) {
            userName = userNameParts[0] + "@" + userNameParts[1];
            tenant = userNameParts[2];
        } else {
            userName = userNameParts[0];
            tenant = userNameParts[1];
        }
        if (logger.isDebugEnabled()) {
            logger.debug("userName: " + userName);
            logger.debug("tenant: " + tenant);
        }
        // ldapmt PATCH END
    	
        if (logger.isTraceEnabled())
        {
            logger.trace("Authentication for user: " + AuthenticationUtil.maskUsername(userName));
        }
        // Distinguished name of user.
        String userDN;
        
        AuthenticationDiagnostic diagnostic = new AuthenticationDiagnostic();
        
        if(userNameFormat == null)
        {
            // If we aren't using a fixed name format, do a search to resolve the user DN
            userDN = ldapNameResolver.resolveDistinguishedName(userName, diagnostic);
            
            Object[] params = {userName, userDN};
            diagnostic.addStep(AuthenticationDiagnostic.STEP_KEY_LDAP_LOOKEDUP_USER, true, params);
        }
        else
        // Otherwise, use the format, but disallow leading or trailing whitespace in the user ID as this can result in
        // ghost users (MNT-2597)
        {
            if (!userName.equals(userName.trim()))
            {
                throw new AuthenticationException("Invalid user ID with leading or trailing whitespace");
            }
            // we are using a fixed name format, 
            userDN = String.format(
                    userNameFormat, new Object[]
                    {
                        escapeUserName(userName, escapeCommasInBind)
                    });
            Object[] params = {userName, userDN, userNameFormat};
            diagnostic.addStep(AuthenticationDiagnostic.STEP_KEY_LDAP_FORMAT_USER, true, params);
        }

        InitialDirContext ctx = null;
        try
        {
            ctx = ldapInitialContextFactory.getInitialDirContext(userDN, new String(password), diagnostic);
            
            // ldapmt PATCH START
            handleUser(userName, tenant);
            userName = userName + "@" + tenant;
            // ldapmt PATCH END            

            // Authentication has been successful.
            // Set the current user, they are now authenticated.
            setCurrentUser(escapeUserName(userName, escapeCommasInUid));

        }
        finally
        {
            if (ctx != null)
            {
                try
                {
                    ctx.close();
                }
                catch (NamingException e)
                {
                    clearCurrentSecurityContext();
                    throw new AuthenticationException("Failed to close connection", e);
                }
            }
        }
    }

    private static String escapeUserName(String userName, boolean escape)
    {
        if (escape)
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < userName.length(); i++)
            {
                char c = userName.charAt(i);
                if (c == ',')
                {
                    sb.append('\\');
                }
                sb.append(c);
            }
            return sb.toString();

        }
        else
        {
            return userName;
        }

    }

    @Override
    protected boolean implementationAllowsGuestLogin()
    {
        return true;
    }
    
    private String id = "default";
    
    /**
     * Set the unique name of this ldap authentication component e.g. "managed,ldap1"
     * 
     * @param id String
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Get the unique name of this ldap authentication component e.g. "managed,ldap1";
     * @return the unique name of this ldap authentication component
     */
    String getId()
    {
        return id;
    }
    
	// ldapmt PATCH START
	private void handleUser(final String userName, final String tenant) {

		transactionService.getRetryingTransactionHelper()
				.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {

					@Override
					public Void execute() throws Throwable {
						AuthenticationUtil.runAs(new RunAsWork<Void>() {

							@Override
							public Void doWork() throws Exception {

								if (personService.personExists(userName + "@" + tenant)) {
									return null;
								}

								if (!userRegistrySynchronizer.createMissingPerson(userName + "@" + tenant)) {
									throw new AuthenticationException("User \"" + userName
											+ "\" does not exist and could not be created in Alfresco");
								}

								return null;
							}
						}, authenticationContext.getSystemUserName(tenant));
						return null;
					}
				}, false, true);

	}
	
	@Override
	public void setAuthenticationContext(AuthenticationContext authenticationContext) {
		super.setAuthenticationContext(authenticationContext);
		this.authenticationContext = authenticationContext;
	}

	@Override
	public void setUserRegistrySynchronizer(UserRegistrySynchronizer userRegistrySynchronizer) {
		super.setUserRegistrySynchronizer(userRegistrySynchronizer);
		this.userRegistrySynchronizer = userRegistrySynchronizer;
	}

	@Override
	public void setPersonService(PersonService personService) {
		super.setPersonService(personService);
		this.personService = personService;
	}

	@Override
	public void setTransactionService(TransactionService transactionService) {
		super.setTransactionService(transactionService);
		this.transactionService = transactionService;
	}
	// ldapmt PATCH END    
}
