package ru.morcool.sender;

/** Permission and live service connection are different states. */
public final class AccessState {
    public enum State { DISABLED, CONNECTING, READY }
    public static State resolve(boolean enabled,boolean connected){
        if(!enabled)return State.DISABLED;
        return connected?State.READY:State.CONNECTING;
    }
    public static String description(State state){
        switch(state){
            case READY:return "Разрешение выдано. Служба подключена. Рассылка запускается только вами.";
            case CONNECTING:return "Разрешение выдано, но служба ещё не подключена. Если состояние не меняется, выключите и снова включите Morcool Sender в специальных возможностях.";
            default:return "Служба выключена в специальных возможностях Android. Ручной режим доступен без неё.";
        }
    }
}
