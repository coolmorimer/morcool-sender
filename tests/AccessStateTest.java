import ru.morcool.sender.AccessState;
public final class AccessStateTest {
 public static void main(String[] args){
  check(AccessState.resolve(false,false)==AccessState.State.DISABLED,"permission off");
  check(AccessState.resolve(true,false)==AccessState.State.CONNECTING,"granted but stopped");
  check(AccessState.resolve(true,true)==AccessState.State.READY,"connected");
  check(AccessState.resolve(false,true)==AccessState.State.DISABLED,"stale reference after revocation");
  check(AccessState.description(AccessState.State.CONNECTING).startsWith("Разрешение выдано"),"never ask to grant an existing permission");
  System.out.println("PASS: 5 accessibility-state regression checks");
 }
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
}
