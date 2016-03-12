package co.paralleluniverse.fibers.shiro;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.HashSet;
import java.util.Set;

/**
 * @author rodedb
 */
public class FiberedRealm extends AuthorizingRealm {

    @Suspendable
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        final Set<String> roles = new HashSet<>();
        roles.add("roleA");
        roles.add("roleB");
        final SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo(roles);
        authorizationInfo.addStringPermission("resource:actionA");
        authorizationInfo.addStringPermission("resource:actionB");
        try {
            Fiber.sleep(10);
        } catch (InterruptedException e) {
            return null;
        } catch (SuspendExecution suspendExecution) {
            throw new AssertionError("Should not happen");
        }

        return authorizationInfo;
    }

    @Suspendable
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) throws AuthenticationException {
        final SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), "FiberedRealm");
        try {
            Fiber.sleep(10);
        } catch (InterruptedException e) {
            return null;
        } catch (SuspendExecution suspendExecution) {
            throw new AssertionError("Should not happen");
        }
        return authenticationInfo;
    }
}
