package com.seguridad.bovedachat.service;
import com.seguridad.bovedachat.domain.*;
import com.seguridad.bovedachat.repository.*;
import com.seguridad.bovedachat.web.Views.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.*;
@Service
public class ChatService {
    private final ConversationRepository conversations; private final AccountRepository users; private final MessageRepository messages;
    private final FileService files; private final CryptoService crypto; private final AuditService audit;
    public ChatService(ConversationRepository conversations,AccountRepository users,MessageRepository messages,FileService files,CryptoService crypto,AuditService audit) {
        this.conversations=conversations;this.users=users;this.messages=messages;this.files=files;this.crypto=crypto;this.audit=audit;
    }
    public Person person(Account a) { return new Person(a.getId(),a.getUsername(),a.getDisplayName()); }
    @Transactional(readOnly=true)
    public List<Person> search(String prefix,Account actor) {
        String query=prefix==null?"":prefix.trim().toLowerCase(Locale.ROOT).replaceFirst("^@","");
        if(!query.matches("[a-z0-9_]{2,24}")) return List.of();
        return users.search(query,actor.getId(),PageRequest.of(0,10)).stream().map(this::person).toList();
    }
    @Transactional
    public ConversationView start(String username,Account actor) {
        Account peer=users.findByUsername(username.trim().toLowerCase(Locale.ROOT).replaceFirst("^@","")).filter(Account::getEnabled).orElseThrow(AppException::missing);
        if(peer.getId().equals(actor.getId())) throw AppException.invalid("Elige otra persona para conversar.");
        boolean actorFirst=actor.getId().toString().compareTo(peer.getId().toString())<0;
        Account low=actorFirst?actor:peer,high=actorFirst?peer:actor;
        Conversation c=conversations.findByUserLow_IdAndUserHigh_Id(low.getId(),high.getId()).orElseGet(()->{
            Conversation created=new Conversation();created.setId(UUID.randomUUID());created.setUserLow(low);created.setUserHigh(high);created.setUpdatedAt(Instant.now());
            conversations.saveAndFlush(created); return created;
        });
        return view(c,actor);
    }
    @Transactional(readOnly=true)
    public List<ConversationView> list(Account actor) { return conversations.listFor(actor.getId(),PageRequest.of(0,100)).stream().map(c->view(c,actor)).toList(); }
    private boolean isLow(Conversation c,Account actor) { return c.getUserLow().getId().equals(actor.getId()); }
    private void authorized(Conversation c,Account actor) {
        if(!isLow(c,actor) && !c.getUserHigh().getId().equals(actor.getId())) {
            audit.denied(actor.getUsername(),"CHAT_ACCESS_DENIED","Conversacion sin permiso"); throw AppException.missing();
        }
    }
    private ConversationView view(Conversation c,Account actor) {
        boolean low=isLow(c,actor);
        long read=low?c.getLowReadId():c.getHighReadId();
        return new ConversationView(c.getId(),person(low?c.getUserHigh():c.getUserLow()),messages.countByConversation_IdAndSender_IdNotAndIdGreaterThan(c.getId(),actor.getId(),read),low?c.getHighReadId():c.getLowReadId(),c.getUpdatedAt());
    }
    @Transactional(readOnly=true)
    public MessagePage history(UUID id,Account actor,long after,long before) {
        if(after<0 || before<0 || (after>0 && before>0)) throw AppException.invalid("El cursor no es valido.");
        Conversation c=conversations.findById(id).orElseThrow(AppException::missing); authorized(c,actor);
        List<ChatMessage> found;
        if(after>0) found=messages.findByConversation_IdAndIdGreaterThanOrderByIdAsc(id,after,PageRequest.of(0,51));
        else if(before>0) found=messages.findByConversation_IdAndIdLessThanOrderByIdDesc(id,before,PageRequest.of(0,51));
        else found=messages.findByConversation_IdOrderByIdDesc(id,PageRequest.of(0,51));
        boolean more=found.size()>50; var slice=new ArrayList<>(found.subList(0,Math.min(50,found.size())));
        if(after==0) Collections.reverse(slice);
        return new MessagePage(slice.stream().map(m->messageView(m,actor)).toList(),isLow(c,actor)?c.getHighReadId():c.getLowReadId(),after==0 && more,after>0 && more);
    }
    @Transactional
    public MessageView send(UUID id,Account actor,String text,UUID attachmentId) {
        String body=text==null?"":text.trim();
        if(body.length()>4000) throw AppException.invalid("El mensaje admite hasta 4000 caracteres.");
        if(body.isEmpty() && attachmentId==null) throw AppException.invalid("Escribe un mensaje o adjunta un archivo.");
        Conversation c=conversations.lockById(id).orElseThrow(AppException::missing); authorized(c,actor);
        StoredFile attachment=attachmentId==null?null:files.ownedForShare(attachmentId,actor);
        ChatMessage m=new ChatMessage();m.setConversation(c);m.setSender(actor);m.setAttachment(attachment);m.setCreatedAt(Instant.now());
        if(!body.isEmpty()) m.setCiphertext(crypto.encryptText(body,context(c.getId(),actor.getId())));
        messages.saveAndFlush(m); c.setUpdatedAt(m.getCreatedAt());
        if(attachment!=null) audit.record(actor.getUsername(),"FILE_SHARED","Documento "+attachment.getId());
        return messageView(m,actor);
    }
    @Transactional
    public void read(UUID id,Account actor,long lastSeenId) {
        if(lastSeenId<0) throw AppException.invalid("El cursor no es valido.");
        Conversation c=conversations.lockById(id).orElseThrow(AppException::missing); authorized(c,actor);
        long last=Math.min(lastSeenId,messages.lastId(id));
        if(isLow(c,actor)) c.setLowReadId(Math.max(c.getLowReadId(),last)); else c.setHighReadId(Math.max(c.getHighReadId(),last));
    }
    private String context(UUID conversation,UUID sender) { return "message:"+conversation+":"+sender; }
    private MessageView messageView(ChatMessage m,Account actor) {
        String text=m.getCiphertext()==null?"":crypto.decryptText(m.getCiphertext(),context(m.getConversation().getId(),m.getSender().getId()));
        return new MessageView(m.getId(),m.getSender().getId(),text,m.getAttachment()==null?null:files.view(m.getAttachment(),actor),m.getCreatedAt());
    }
}
