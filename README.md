current version: 1.0.0-SNAPSHOT

# Alfresco LDAP Multi-Tenancy Subsystem

This Alfresco module provides a subsystem that makes it possible to use LDAP/AD
authentication and synchronization of authorities while multi-tenancy is
enabled.

## Supported Alfresco Version

This module is build for and tested with Alfresco 6.1.

## Build

To build this module Java 8+ and Maven are required. Clone this repository,
enter the cloned directory. Then build the module:

```bash
mvn package
```

This will create the module as `target/ldapmt-{version}.jar`.

## Installation

Copy the module JAR file into the directory `module/platform` of your Alfresco
installation. After configuring the module, restart Alfresco.

## Configuration

### Create Subsystems

Create an authentication subsystem of type `ldapmat` and give it at name, for
example `ldapmt1`:

```
alfresco/extension/subsystems
└── Authentication
    └── ldapmt
        └── ldapmt1
            └── ldapmt-authentication.properties
```

Create a default synchronization subsystem:

```
alfresco/extension/subsystems
└── Synchronization
    └── default
        └── default
            └── ldapmt-synchronization.properties
```

### Configure Authentication Chain

To use the subsystem in the authentication chain, add a `ldapmt` instance to it.

Example:

```properties
# The name of the `ldaptmt` authentication subsystem is `ldapmt1` which
# corresponds to the name of the directory created above.
authentication.chain=alfrescoNtlm1:alfrescoNtlm,ldapmt1:ldapmt
```

**Important:** Note that the name of the `ldapmt` instance (`ldapmt1` in the
example above) determines the Zone into which authorities are synchronized. If
you change this name, authorities will be deleted and created as new
authorities!

### Configuration Properties

This subsystems uses properties similar to the usual synchronization and
authorization properties. The property names use the prefix `ldapmt` instead of
`ldap`.

**Important:** If you already have synchronization and authorization properties
configured, note that this module uses **different** property names. The prefix
`ldapmt` is required. Properties with the prefix `ldap` have no effect for this
module!

The default values for all properties are set in two files inside the module,
[`ldapmt-authentication.properties`](src/main/resources/alfresco/subsystems/Authentication/ldapmt/ldapmt-authentication.properties)
and [`ldapmt-synchronization.properties`](src/main/resources/alfresco/subsystems/Synchronization/default/ldapmt-synchronization.properties).

Edit `ldapmt-authentication.properties`:

```properties
# The URL for the LDAP server.
ldapmt.authentication.java.naming.provider.url=ldap://ldap.example.com:389

# The DN of the principal to use for synchronization.
ldapmt.synchronization.java.naming.security.principal=cn=admin,dc=example,dc=com

# The password of the principal.
ldapmt.synchronization.java.naming.security.credentials=secret-password
```

Edit `ldapmt-synchronization.properties`:

```properties
# When to synchronize authorities.
synchronization.import.cron=0 0 * * * ?

# LDAP search bases for people and groups.
ldapmt.synchronization.userSearchBase=ou=people,dc=example,dc=com
ldapmt.synchronization.groupSearchBase=o={tenant},DC=example,DC=com

# LDAP queries for people and groups.
ldapmt.synchronization.personQuery=(&(objectclass=inetOrgPerson)(memberOf=CN={TENANT}_USERS,o={tenant},DC=example,DC=com))
ldapmt.synchronization.personDifferentialQuery=(&(objectclass=inetOrgPerson)(!(modifyTimestamp<={0}))(memberOf=CN={TENANT}_USERS,o={tenant},dc=example,dc=com))
ldapmt.synchronization.groupQuery=(objectclass\=groupOfNames)
ldapmt.synchronization.groupDifferentialQuery=(&(objectclass\=groupOfNames)(!(modifyTimestamp<\={0})))

# The name of the authentication subsystem for which this subsystem shall synchronize authorities.
ldapmt.synchronization.authenticationSubsystemName=ldapmt1
```

#### Properties with Tenant Placeholders

This subsystem assumes that authorities are organized by tenant in the LDAP/AD.
If the authorities of the tenant `tenant` are to be synchronized, the LDAP
queries are constructed using `tenant` and templates. This template mechanism
applies to all configuration properties whose values contain the placeholders
`{tenant}` or `{TENANT}`.

To expand such property values, the name of the tenant to synchronize is
replaced inside the property values. The placeholder `{tenant}` is replaced
with the name of the tenant, the placeholder `{TENANT}` is replaced with the
name of the tenant in upper case.

*Example:*

The tenant name `tenant1` used together with the the above templated property
values results in these effective values:

* `(&(objectclass=inetOrgPerson)(memberOf=CN=TENANT1_USERS,o=tenant1,DC=example,DC=com))`
* `(&(objectclass=inetOrgPerson)(!(modifyTimestamp<={0}))(memberOf=CN=TENANT1_USERS,o=tenant1s,dc=example,dc=com))`

## License

This module is free software: you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version.
