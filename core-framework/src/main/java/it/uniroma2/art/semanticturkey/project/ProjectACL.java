package it.uniroma2.art.semanticturkey.project;

import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class describes the Access Control List for a given {@link Project}. It consists in:
 * <ul>
 * <li>a list of {@link ProjectConsumer}s, together with their access permissions.</li>
 * <li>a "lockable" property, telling if the project associated to this ACL, can be locked for use by a
 * {@link ProjectConsumer}, and with which modality</li>
 * </ul>
 *
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
 */
public class ProjectACL {

    public enum AccessLevel {
        /*
        EXT is an access level reserved for universal access, it means that the project is accessible from a consumer
        if the user has direct access to the project, even if project doesn't grant any level to consumer
        see https://art-uniroma2.atlassian.net/browse/ST-2105
         */
        R, RW, EXT;

        public static boolean resolveAccessibility(AccessLevel requestedLevel, AccessLevel allowedLevel) {
            return requestedLevel == allowedLevel || allowedLevel == AccessLevel.RW;
        }

        /**
         * returns <code>true</code> if the requested level can be accepted by this access level
         *
         * @param requestedLevel
         * @return
         */
        public boolean accepts(AccessLevel requestedLevel) {
            return resolveAccessibility(requestedLevel, this);
        }

        /**
         * returns <code>true</code> if the allowedLevel allows for accepting this level
         *
         * @param allowedLevel
         * @return
         */
        public boolean isAcceptedBy(AccessLevel allowedLevel) {
            return resolveAccessibility(this, allowedLevel);
        }
    }


    public enum LockLevel {
        W, R, NO;

        public static boolean resolveLocking(LockLevel requestedLevel, LockLevel allowedLevel) {
            return requestedLevel == allowedLevel || requestedLevel == NO || allowedLevel == R;
        }

        public boolean accepts(LockLevel requestedLevel) {
            return resolveLocking(requestedLevel, this);
        }

        public boolean isAcceptedBy(LockLevel allowedLevel) {
            return resolveLocking(this, allowedLevel);
        }

    }

    private Map<String, AccessLevel> acl = new HashMap<>();
    private LockLevel lockLevel;

    private Project project;

    public static final String ACL = "acl.acl";
    public static final String LOCKLEVEL = "acl.lockLevel";

    public static final String WILDCARD_CONSUMER = "*";

    /**
     * this constructors takes as input a list of comma separated values of the form:<br/>
     * <code>&lt;ProjectConsumer&gt;:&lt;AccessLevel&gt;</code>
     * A special "*" ProjectConsumer can be used as wildcard for "every consumer" (except SYSTEM)
     *
     * @param project
     */
    ProjectACL(Project project) {
        this.project = project;
        String aclSerialization = project.getProperty(ACL);
        String lockLevelSerialization = project.getProperty(LOCKLEVEL);

        if (aclSerialization != null) {
            String[] acis = aclSerialization.split(",");
            for (String aci : acis) {
                String[] acisplit = aci.split(":");
                acl.put(acisplit[0], AccessLevel.valueOf(acisplit[1]));
            }
        } else
            // if no ACL is specified, it defaults to {SYSTEM, RW}
            acl.put(ProjectConsumer.SYSTEM.getName(), AccessLevel.RW);

        if (lockLevelSerialization != null)
            this.lockLevel = LockLevel.valueOf(lockLevelSerialization);
        else
            // if no lock is specified, it defaults to R
            this.lockLevel = LockLevel.R;
    }

    // ACCESS CONTROL

    public boolean isLockable() {
        return (lockLevel != LockLevel.NO);
    }

    /**
     * this method tells if the project that is owning this ACL can be accessed with access level
     * <code>reqLevel</code> from the given {@link ProjectConsumer} <code>consumer</code>. <br/>
     * This is not considering the runtime status of the project, in case it has already been accessed by
     * another {@link ProjectConsumer}
     *
     * @param consumer
     * @param reqLevel
     * @return
     */
    private boolean allowsAccessWith(ProjectConsumer consumer, AccessLevel reqLevel) {
        AccessLevel storedLevel = acl.get(consumer.getName());
        if (storedLevel == null) { //no access level set in ACL from project to consumer
            if (isUniversallyAccessible(AccessLevel.EXT)) {
                /* universal access set to EXT =>
                project is still accessible from consumer if user has direct access to project */
                STUser user = UsersManager.getLoggedUser();
                if (user.isAdmin()) {
                    return true; //admin has access to all projects
                } else {
                    //non-admin has access if he has (at least) a role assigned in project
                    ProjectUserBinding puBinding = ProjectUserBindingsManager.getPUBinding(user, project);
                    return puBinding != null && puBinding.getRoles().size() > 0;
                }
            } else {
                return false;
            }
		}
        return reqLevel.isAcceptedBy(storedLevel);
    }

    /**
     * this method tells if the project that is owning this ACL can be locked with the desired lock level
     * <code>reqLevel</code>. <br/>
     * This is not considering the runtime status of the project, in case it has already been accessed by
     * another {@link ProjectConsumer}
     *
     * @param reqLevel
     * @return
     */
    private boolean isLockableWithLevel(LockLevel reqLevel) {
        return reqLevel.isAcceptedBy(lockLevel);
    }

    /**
     * This method returns the AccessLevel granted from the project that is owning this ACL to the given
     * consumer. <code>null</code> if no AccessLevel is specified for the consumer.
     *
     * @param consumer
     * @return
     */
    public AccessLevel getAccessLevelForConsumer(ProjectConsumer consumer) {
        return acl.get(consumer.getName());
    }

    /**
     * Returns the AccessLevel granted from the project that is owning this ACL to all the consumers ("*" consumer).
     * <code>null</code> if no AccessLevel is specified.
     *
     * @return
     */
    public AccessLevel getUniversalAccessLevel() {
        return acl.get(WILDCARD_CONSUMER);
    }

    /**
     * Returns the lock level of the project that is owning this ACL
     *
     * @return
     */
    public LockLevel getLockLevel() {
        return lockLevel;
    }

    /**
     * this method tells if the project that is owning this ACL can be accessed with the desired access/lock
     * level specifications. <br/>
     * This is not considering the runtime status of the project, in case it has already been accessed by
     * another {@link ProjectConsumer}
     *
     * @param consumer
     * @param reqAccessLevel
     * @param reqLock
     * @return
     */
    public boolean isAccessibleFrom(ProjectConsumer consumer, AccessLevel reqAccessLevel, LockLevel reqLock) {
        /*
        check if:
        - project allows access to consumer with reqAccessLevel OR
          project is universally accessible with reqAccessLevel
        AND
        - project is lockable with reqLock
         */
        return ((allowsAccessWith(consumer, reqAccessLevel) || isUniversallyAccessible(reqAccessLevel)) && isLockableWithLevel(reqLock));
    }

    /**
     * Tells if the project that is owning this ACL can be universally accessed with any level
     *
     * @return
     */
    public boolean isUniversallyAccessible() {
        return isUniversallyAccessible(null);
    }

    /**
     * Tells if the project that is owning this ACL can be universally accessed with the given level
     *
     * @return
     */
    public boolean isUniversallyAccessible(AccessLevel reqAccessLevel) {
        AccessLevel accessLevelAnyProject = acl.get(WILDCARD_CONSUMER);
        if (accessLevelAnyProject != null) {
            if (reqAccessLevel != null) {
                return reqAccessLevel.isAcceptedBy(accessLevelAnyProject);
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * this method tells if consumer <code>consumer</code> is listed in the ACL of the current project. This
     * does not tell anything about the {@link AccessLevel} nor the {@link LockLevel} which would be accepted
     * for a request from consumer
     *
     * @param consumer
     * @return
     */
    public boolean hasInACL(ProjectConsumer consumer) {
        return (allowsAccessWith(consumer, AccessLevel.R));

    }

    // UPDATE

    public void grantAccess(ProjectConsumer consumer, AccessLevel reqAccessLevel)
            throws ProjectUpdateException, ReservedPropertyUpdateException {
        acl.put(consumer.getName(), reqAccessLevel);
        saveACL();
    }

    /**
     * Grants universally (to all consumer) the given access level
     *
     * @param reqAccessLevel
     * @throws ProjectUpdateException
     * @throws ReservedPropertyUpdateException
     */
    public void grantUniversalAccess(AccessLevel reqAccessLevel)
            throws ProjectUpdateException, ReservedPropertyUpdateException {
        acl.put(WILDCARD_CONSUMER, reqAccessLevel);
        saveACL();
    }

    public void revokeAccess(ProjectConsumer consumer) throws ProjectUpdateException, ReservedPropertyUpdateException {
        acl.remove(consumer.getName());
        saveACL();
    }

    /**
     * Revokes the universal access level
     *
     * @throws ProjectUpdateException
     * @throws ReservedPropertyUpdateException
     */
    public void revokeUniversalAccess() throws ProjectUpdateException, ReservedPropertyUpdateException {
        acl.remove(WILDCARD_CONSUMER);
        saveACL();
    }

    public void setLockableWithLevel(LockLevel lockLevel) throws ProjectUpdateException,
            ReservedPropertyUpdateException {
        this.lockLevel = lockLevel;
        saveLock();
    }

    // SERIALIZATION


    /**
     * serialization of the ACL for a specific consumer.
     *
     * @param consumerName
     * @param accessLevel
     * @return
     */
    public static String serializeACL(String consumerName, AccessLevel accessLevel) {
        Map<String, AccessLevel> acl = new HashMap<>();
        acl.put(consumerName, accessLevel);
        return serializeACL(acl);
    }

    /**
     * serialization of the whole ACL (of a project), containing all of its consumers and access levels
     *
     * @param acl
     * @return
     */
    public static String serializeACL(Map<String, AccessLevel> acl) {
        StringBuilder aclString = new StringBuilder();
        for (Entry<String, AccessLevel> entry : acl.entrySet()) {
            aclString.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        aclString.deleteCharAt(aclString.length() - 1);
        return aclString.toString();
    }


    private void saveACL() throws ProjectUpdateException, ReservedPropertyUpdateException {

        project.setProperty(ACL, serializeACL(acl));

    }

    private void saveLock() throws ProjectUpdateException, ReservedPropertyUpdateException {
        project.setProperty(LOCKLEVEL, lockLevel.toString());
    }

}
