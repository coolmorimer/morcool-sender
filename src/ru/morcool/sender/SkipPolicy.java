package ru.morcool.sender;
import java.util.Locale;

public final class SkipPolicy {
    public static boolean unavailable(String message){
        String s=message==null?"":message.toLowerCase(Locale.ROOT).replace('\u2019','\'').replace('ё','е');
        if(s.contains("not on whatsapp")||s.contains("isn't on whatsapp")||s.contains("is not on whatsapp")||s.contains("not registered on whatsapp"))return true;
        if(s.contains("phone number")&&(s.contains("invalid")||s.contains("not valid")||s.contains("not found")||s.contains("couldn't find")||s.contains("could not find")))return true;
        if(s.contains("номер")&&(s.contains("не зарегистрирован")||s.contains("не найден")||s.contains("не удалось найти")||s.contains("недействител")||s.contains("неверн")||s.contains("некорректн")||s.contains("нет в whatsapp")||s.contains("не использует whatsapp")))return true;
        return s.contains("пользовател")&&(s.contains("не найден")||s.contains("не удалось найти"));
    }
    public static boolean skipTimeout(boolean sendClicked,boolean whatsappSeen){return !sendClicked&&whatsappSeen;}
}
