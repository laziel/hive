package models;

import models.enumeration.NotificationType;
import models.enumeration.ResourceType;
import models.enumeration.State;
import models.resource.Resource;
import org.joda.time.DateTime;
import play.Configuration;
import play.db.ebean.Model;
import play.i18n.Messages;
import playRepository.Commit;

import javax.persistence.*;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
public class NotificationEvent extends Model {
    private static final long serialVersionUID = 1L;

    private static final int NOTIFICATION_DRAFT_TIME_IN_MILLIS = Configuration.root()
            .getMilliseconds("application.notification.draft-time", 30 * 1000L).intValue();

    @Id
    public Long id;

    public static Finder<Long, NotificationEvent> find = new Finder<Long,
            NotificationEvent>(Long.class, NotificationEvent.class);

    public String title;

    @Lob
    public String message;

    public Long senderId;

    @ManyToMany(cascade = CascadeType.ALL)
    public Set<User> receivers;

    @Temporal(TemporalType.TIMESTAMP)
    public Date created;

    public String urlToView;

    @Enumerated(EnumType.STRING)
    public ResourceType resourceType;

    public Long resourceId;

    @Enumerated(EnumType.STRING)
    public NotificationType type;

    @Lob
    public String oldValue;

    @Lob
    public String newValue;

    @OneToOne(mappedBy="notificationEvent", cascade = CascadeType.ALL)
    public NotificationMail notificationMail;

    public static String formatReplyTitle(Project project, Commit commit) {
        return String.format("Re: [%s] %s (#%s)",
                project.name, commit.getShortMessage(), commit.getShortId());
    }

    public static String formatReplyTitle(AbstractPosting posting) {
        return String.format("Re: [%s] %s (#%d)",
                posting.project.name, posting.title, posting.getNumber());
    }

    public static String formatNewTitle(AbstractPosting posting) {
        return String.format("[%s] %s (#%d)",
                posting.project.name, posting.title, posting.getNumber());
    }

    public String getOldValue() {
        return oldValue;
    }

    @Transient
    public static Set<User> getMentionedUsers(String body) {
        Matcher matcher = Pattern.compile("@" + User.LOGIN_ID_PATTERN).matcher(body);
        Set<User> users = new HashSet<>();
        while(matcher.find()) {
            users.add(User.findByLoginId(matcher.group().substring(1)));
        }
        users.remove(User.anonymous);
        return users;
    }

    @Transient
    public String getMessage() {
        if (message != null) {
            return message;
        }

        switch (type) {
        case ISSUE_STATE_CHANGED:
            if (newValue.equals(State.CLOSED.state())) {
                return Messages.get("notification.issue.closed");
            } else {
                return Messages.get("notification.issue.reopened");
            }
        case ISSUE_ASSIGNEE_CHANGED:
            if (newValue == null) {
                return Messages.get("notification.issue.unassigned");
            } else {
                return Messages.get("notification.issue.assigned", newValue);
            }
        case NEW_ISSUE:
        case NEW_POSTING:
        case NEW_COMMENT:
            return newValue;
        default:
            return null;
        }
    }

    public User getSender() {
        return User.find.byId(this.senderId);
    }
    
    public Resource getResource() {
        Resource resource = null;

        switch(resourceType) {
            case ISSUE_POST:
                resource = Issue.finder.byId(resourceId).asResource();
                break;
            case ISSUE_COMMENT:
                resource = IssueComment.find.byId(resourceId).asResource();
                break;
            case NONISSUE_COMMENT:
                resource = PostingComment.find.byId(resourceId).asResource();
                break;
            case LABEL:
                resource = Label.find.byId(resourceId).asResource();
                break;
            case BOARD_POST:
                resource = Posting.finder.byId(resourceId).asResource();
                break;
            case USER:
                resource = null;
                break;
            case PROJECT:
                resource = Project.find.byId(resourceId).asResource();
                break;
            case ATTACHMENT:
                resource = Attachment.find.byId(resourceId).asResource();
                break;
            case MILESTONE:
                resource = Milestone.find.byId(resourceId).asResource();
                break;
            case CODE_COMMENT:
                resource = CodeComment.find.byId(resourceId).asResource();
                break;
            default:
                throw new IllegalArgumentException(getInvalidResourceTypeMessage(resourceType));
        }

        return resource;
    }

    private static String getInvalidResourceTypeMessage(ResourceType resourceType) {
        if (EnumSet.allOf(ResourceType.class).contains(resourceType)) {
            return "Unsupported resource type " + resourceType;
        } else {
            return "Unknown resource type " + resourceType;
        }
    }

    public Project getProject() {
        switch(resourceType) {
        case ISSUE_ASSIGNEE:
            return Assignee.finder.byId(resourceId).project;
        default:
            Resource resource = getResource();
            if (resource != null) {
                return resource.getProject();
            } else {
                return null;
            }
        }
    }

    public boolean resourceExists() {
        Finder<Long, ? extends Model> finder = null;

        switch(resourceType) {
            case ISSUE_POST:
                finder = Issue.finder;
                break;
            case ISSUE_ASSIGNEE:
                finder = Assignee.finder;
                break;
            case ISSUE_COMMENT:
                finder = IssueComment.find;
                break;
            case NONISSUE_COMMENT:
                finder = PostingComment.find;
                break;
            case LABEL:
                finder = Label.find;
                break;
            case BOARD_POST:
                finder = Posting.finder;
                break;
            case USER:
                finder = User.find;
                break;
            case PROJECT:
                finder = Project.find;
                break;
            case ATTACHMENT:
                finder = Attachment.find;
                break;
            case MILESTONE:
                finder = Milestone.find;
                break;
            case CODE_COMMENT:
                finder = CodeComment.find;
                break;
            default:
                throw new IllegalArgumentException(getInvalidResourceTypeMessage(resourceType));
        }

        return finder.byId(resourceId) != null;
    }

    public static void add(NotificationEvent event) {
        if (event.notificationMail == null) {
            event.notificationMail = new NotificationMail();
            event.notificationMail.notificationEvent = event;
        }

        Date draftDate = DateTime.now().minusMillis(NOTIFICATION_DRAFT_TIME_IN_MILLIS).toDate();

        NotificationEvent lastEvent = NotificationEvent.find.where()
                .eq("resourceId", event.resourceId)
                .eq("resourceType", event.resourceType)
                .gt("created", draftDate)
                .orderBy("id desc").setMaxRows(1).findUnique();

        if (lastEvent != null) {
            if (lastEvent.type.equals(event.type)) {
                String oldValue = lastEvent.getOldValue();

                if (event.senderId.equals(lastEvent.senderId)) {
                    lastEvent.delete();
                }

                if ((event.newValue == null && oldValue == null)
                        || event.newValue.equals(oldValue)) {
                    // No need to add this event because the event just cancels the last event
                    // which has just been deleted.
                    return;
                }
            }
        }

        event.save();
    }

    public static void deleteBy(Resource resource) {
        for (NotificationEvent event : NotificationEvent.find.where().where().eq("resourceType",
                resource.getType()).eq("resourceId", resource.getId()).findList()) {
            event.delete();
        }
    }
}
