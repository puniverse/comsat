Class Replacement
=================

Let `R` replacing `O` be as follows:

```
class O(riginal) extends O(riginal)B(ase) implements I1 ... In { F1 ... Fn, M1 ... Mn }
class R(eplacement) extends R(eplacement)B(ase) implements I1 ... In { ... }
```

`RB` already replaces `OB`, else recur replacement up the hierarchy until a common base is reached (`Object` in the worst case). Replacement means either _proxying_ or _rewriting_ all features.

Need for rewriting
------------------

A feature in `O` needs to be _rewritten_ in `R` iff at least one of the following conditions is met:

0. It is better to change it because proxying it requires more code
1. It must be changed because the old implementation is inadequate
2. It depends on features of `O` that have been rewritten in `R` and it's not possible to _forward_ its access to them; _forwarding_ is defined later on
3. Features already rewritten in `R` depend on it and can't _access_ it, not even through an intermediate _proxy_; _access_ is defined later on

Forwarding references
---------------------

- Field references can't be forwarded
- Class references can't be forwarded
- A method reference can be forwarded through instrumentation iff:
  - It is acceptable to redefine the class hosting the referenced feature at runtime (it can't be done for single instances)
- A Method reference can be forwarded through subclassing iff
  - The class hosting the method is not final
  - The class hosting the method is not private
  - The method is not final
  - The method is not private
  - Creation of the instance hosting the method can be performed with the subclass runtime type

Proxying
--------

Proxying means providing a _proxy_, that is features whose sole purpose is to allow access to features of another instance.
Proxy rules are exactly the same as access rules.

In the context of replacement, a feature that doesn't need to be rewritten must be proxied.

Accessing
---------

A prerequisite for direct access to features of an instance (either by the replacement class or by an intermediate proxy) is holding an instance reference.

- Private features can't be accessed
- Package features can be accessed iff the accessor is located in the same package
- Protected features can be accessed iff either
  - The accessor is located in the same package
  - The accessor inherits from the class hosting the feature to be accessed, but still can access only protected features with own instance reference (and not others')
- Public features can always be accessed