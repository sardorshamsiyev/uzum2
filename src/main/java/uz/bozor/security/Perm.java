package uz.bozor.security;

import org.springframework.stereotype.Component;
import uz.bozor.entity.Role;
import uz.bozor.entity.User;

/** Ega/admin huquqlari bitta joyda. */
@Component
public class Perm {
    /** actor target ustida (bloklash, chiqarish, admin qilish) amal qila oladimi? */
    public boolean canAct(User actor, User target) {
        if (target.getRole() == Role.OWNER) return false;          // egaga hech kim tegolmaydi
        if (target.getId().equals(actor.getId())) return false;    // o'ziga emas
        if (actor.getRole() == Role.OWNER) return true;            // ega: admin va userga
        return actor.getRole() == Role.ADMIN && target.getRole() == Role.USER; // admin: faqat userga
    }
}
