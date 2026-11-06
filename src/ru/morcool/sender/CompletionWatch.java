package ru.morcool.sender;

/** A cleared Android editor can expose its hint as node.text. Confirm once, after two fresh samples. */
public final class CompletionWatch {
    private int emptySamples;
    private boolean emitted;
    public void reset(){emptySamples=0;emitted=false;}
    public static boolean cleared(CharSequence text,CharSequence hint,boolean showingHint){
        String value=normalize(text),placeholder=normalize(hint);
        return value.isEmpty() || showingHint || (!placeholder.isEmpty() && value.equals(placeholder));
    }
    private static String normalize(CharSequence s){return s==null?"":s.toString().replace("\u200e","").replace("\u200f","").replace("\u200b","").trim();}
    public static boolean placeholder(CharSequence text){String s=normalize(text);return s.equals("Сообщение")||s.equals("Написать сообщение")||s.equals("Message")||s.equals("Type a message");}
    public boolean observe(boolean clicked,boolean visible,CharSequence text,CharSequence hint,boolean showingHint){
        return observe(clicked,visible,text,hint,showingHint,true);
    }
    public boolean observe(boolean clicked,boolean visible,CharSequence text,CharSequence hint,boolean showingHint,boolean sendAvailable){
        if(emitted)return false;
        // Some WhatsApp builds expose the empty-field label as real text without any hint metadata.
        // The label alone is not evidence: Send must also have disappeared after our click.
        boolean empty=cleared(text,hint,showingHint)||(!sendAvailable && placeholder(text));
        if(!clicked || !visible || !empty){emptySamples=0;return false;}
        if(++emptySamples<2)return false;
        emitted=true;return true;
    }
}
