# Alfresco LDAP Multi-Tenancy Subsystem

This Alfresco module provides a subsystem that makes it possible to use LDAP/AD
authentication and synchronization of authorities while multi-tenancy is
enabled.

## Build

TODO

## Installation

TODO

## Configuration

### Create Subsystem

TODO

### Configuration Properties

This subsystem assumes that authorities are organized by tenant in the LDAP/AD.
If the authorities of the tenant `tenant` are to be synchronized, the LDAP
queries are constructed using `tenant` and templates. This template mechanism
applies to two configuration properties:

* `ldapmt.synchronization.personQuery`
* `ldapmt.synchronization.personDifferentialQuery`

The default values for these properties are

```properties
ldapmt.synchronization.personQuery=(&(objectclass=inetOrgPerson)(memberOf=CN={TENANT}_USERS,o={tenant},DC=example,DC=com))
ldapmt.synchronization.personDifferentialQuery=(&(objectclass=inetOrgPerson)(!(modifyTimestamp<={0}))(memberOf=CN={TENANT}_USERS,o={tenant},dc=example,dc=com))
```

To construct correct LDAP queries, the name of the tenant to synchronize is
replaced inside the property values. The placeholder `{tenant}` is replaced
with the name of the tenant, the placeholder `{TENANT}` is replaced with the
name of the tenant in upper case.

*Example:*

The tenant name `tenant1` used together with the two default property values
results in these effective values:

* `(&(objectclass=inetOrgPerson)(memberOf=CN=TENANT1_USERS,o=tenant1,DC=example,DC=com))`
* `(&(objectclass=inetOrgPerson)(!(modifyTimestamp<={0}))(memberOf=CN=TENANT1_USERS,o=tenant1s,dc=example,dc=com))`

## License

TODO
