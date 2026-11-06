package ru.morcool.sender;

import java.util.*;
import java.util.regex.*;

/** Pure parsing and rendering; never sends messages. */
public final class Core {
    public static String phone(String raw,String country) {
        if(raw==null || !raw.trim().matches("[+0-9() .\\-]+"))throw new IllegalArgumentException("Некорректный номер: "+raw);
        String s=raw.trim(),d=s.replaceAll("[^0-9]","");
        String c=country.replaceAll("[^0-9]","");
        if(!s.startsWith("+")) {
            if(d.startsWith("00"))d=d.substring(2);
            else if(c.equals("7") && d.length()==11 && d.startsWith("8"))d="7"+d.substring(1);
            else if(c.equals("7") && d.length()==10)d=c+d;
            else if(!(d.startsWith(c) && d.length()>10))d=c+d;
        }
        if(d.length()<8 || d.length()>15 || d.startsWith("0"))throw new IllegalArgumentException("Проверьте международный номер: "+raw);
        return "+"+d;
    }
    public static List<List<String>> csv(String input) {
        input=input.replaceFirst("^\\uFEFF","");
        char sep=',';int commas=0,semis=0,tabs=0;boolean q=false;
        for(int i=0;i<input.length();i++){char c=input.charAt(i);if(c=='"')q=!q;if(!q){if(c=='\n'||c=='\r')break;if(c==',')commas++;if(c==';')semis++;if(c=='\t')tabs++;}}
        if(semis>commas)sep=';';if(tabs>Math.max(semis,commas))sep='\t';
        List<List<String>> out=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();q=false;boolean closed=false;
        for(int i=0;i<input.length();i++) {
            char c=input.charAt(i);
            if(q){if(c=='"'){if(i+1<input.length() && input.charAt(i+1)=='"'){cell.append('"');i++;}else{q=false;closed=true;}}else cell.append(c);}
            else if(c=='"'){if(cell.length()>0||closed)throw new IllegalArgumentException("Ошибка кавычек CSV");q=true;}
            else if(c==sep||c=='\n'||c=='\r') {
                row.add(cell.toString().trim());cell.setLength(0);closed=false;
                if(c!=sep){if(!(row.size()==1 && row.get(0).isEmpty()))out.add(row);row=new ArrayList<>();if(c=='\r' && i+1<input.length() && input.charAt(i+1)=='\n')i++;}
            }else{if(closed && !Character.isWhitespace(c))throw new IllegalArgumentException("Лишний символ после кавычки");cell.append(c);}
            if(out.size()>100000 || cell.length()>32768)throw new IllegalArgumentException("Файл слишком большой для памяти телефона");
        }
        if(q)throw new IllegalArgumentException("Незакрытая кавычка CSV");
        if(cell.length()>0||!row.isEmpty()){row.add(cell.toString().trim());out.add(row);}return out;
    }
    public static String render(String template,Map<String,String> fields) {
        if(template.trim().isEmpty())throw new IllegalArgumentException("Напишите сообщение");
        Matcher m=Pattern.compile("\\{([^{}]+)\\}").matcher(template);StringBuffer b=new StringBuffer();
        while(m.find()){String k=m.group(1);if(!fields.containsKey(k))throw new IllegalArgumentException("Нет поля {"+k+"}");m.appendReplacement(b,Matcher.quoteReplacement(fields.get(k)));}m.appendTail(b);
        if(b.length()>4096)throw new IllegalArgumentException("Сообщение длиннее 4096 символов");return b.toString();
    }
    public static String cell(List<String> r,int i){return i>=0&&i<r.size()?r.get(i):"";}
    public static String quote(String s){return "\""+s.replace("\"","\"\"")+"\"";}
}
