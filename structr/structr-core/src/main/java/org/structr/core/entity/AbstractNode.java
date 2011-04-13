package org.structr.core.entity;

import freemarker.template.Configuration;
import org.structr.core.node.TransactionCommand;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.LinkNodeFactoryCommand;
import org.structr.core.node.NodeRelationshipsCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.common.RelType;
import org.structr.common.RenderMode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
//import org.neo4j.graphdb.*;
//import org.neo4j.index.IndexService;
//import org.neo4j.index.lucene.LuceneFulltextIndexService;
//import org.neo4j.kernel.Traversal;
import org.apache.commons.lang.time.DateUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.structr.common.StructrContext;
import org.structr.common.TemplateHelper;
import org.structr.core.cloud.NodeDataContainer;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.IndexNodeCommand;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.XPath;
import org.structr.core.node.search.Search;

/**
 * 
 * @author amorgner
 * 
 */
public abstract class AbstractNode implements Comparable<AbstractNode> {

    private final static String ICON_SRC = "/images/folder.png";
    private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());
    private static final boolean updateIndexDefault = true;
    // request parameters
    //private HttpServletRequest request = null;
    //private HttpSession session = null;
    private Map<Long, StructrRelationship> securityRelationships = null;
    private List<StructrRelationship> incomingLinkRelationships = null;
    private List<StructrRelationship> outgoingLinkRelationships = null;
    private List<StructrRelationship> incomingChildRelationships = null;
    private List<StructrRelationship> outgoingChildRelationships = null;
    private List<StructrRelationship> outgoingDataRelationships = null;
    private List<StructrRelationship> incomingRelationships = null;
    private List<StructrRelationship> outgoingRelationships = null;
    private List<StructrRelationship> allRelationships = null;

    // ----- abstract methods ----
    public abstract void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);

    public abstract String getIconSrc();
    
    // reference to database node
    protected Node dbNode;
    // dirty flag, true means that some changes are not yet written to the database
    protected boolean isDirty;
    protected Map<String, Object> properties;
    // keys for basic properties (any node should have at least all of the following properties)
    public final static String TYPE_KEY = "type";
    public final static String NAME_KEY = "name";
    public final static String CATEGORIES_KEY = "categories";
    public final static String TITLE_KEY = "title";
//    public final static String LOCALE_KEY = "locale";
    public final static String TITLES_KEY = "titles";
    public final static String POSITION_KEY = "position";
    public final static String NODE_ID_KEY = "nodeId";
    public final static String OWNER_KEY = "owner";
    public final static String CREATED_DATE_KEY = "createdDate";
    public final static String CREATED_BY_KEY = "createdBy";
    public final static String LAST_MODIFIED_DATE_KEY = "lastModifiedDate";
    public final static String VISIBILITY_START_DATE_KEY = "visibilityStartDate";
    public final static String VISIBILITY_END_DATE_KEY = "visibilityEndDate";
    public final static String PUBLIC_KEY = "public";
    public final static String VISIBLE_TO_AUTHENTICATED_USERS_KEY = "visibleToAuthenticatedUsers";
    public final static String HIDDEN_KEY = "hidden";
    public final static String DELETED_KEY = "deleted";
//    public final static String ACL_KEY = "acl";
    //private final static String keyPrefix = "${";
    //private final static String keySuffix = "}";
    private final static String NODE_KEY_PREFIX = "%{";
    private final static String NODE_KEY_SUFFIX = "}";
//    private final static String REQUEST_KEY_PREFIX = "$[";
//    private final static String REQUEST_KEY_SUFFIX = "]";
    private final static String CALLING_NODE_SUBNODES_KEY = "*";
    private final static String CALLING_NODE_SUBNODES_AND_LINKED_NODES_KEY = "#";
    protected Template template;


    /*
     * Helper class for multilanguage titles
     */
    public class Title {

        public final static String LOCALE_KEY = "locale";
        private Locale locale;
        private String title;

        public Title(final Locale locale, final String title) {
            this.locale = locale;
            this.title = title;
        }

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(final Locale locale) {
            this.locale = locale;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }
    }

    public AbstractNode() {
        this.properties = new HashMap<String, Object>();
        isDirty = true;
    }

    public AbstractNode(final Map<String, Object> properties) {
        this.properties = properties;
        isDirty = true;
    }

    public AbstractNode(final NodeDataContainer data) {
        if (data != null) {
            this.properties = data.getProperties();
            isDirty = true;
        }
    }

    public AbstractNode(final Node dbNode) {
        init(dbNode);
    }

    public void init(final Node dbNode) {
        this.dbNode = dbNode;
        isDirty = false;
    }

    private void init(final AbstractNode node) {
        this.dbNode = node.dbNode;
        isDirty = false;
    }

    public void init(final NodeDataContainer data) {
        if (data != null) {
            this.properties = data.getProperties();
            isDirty = true;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof AbstractNode)) {
            return false;
        }
        return ((AbstractNode) o).equals(this);
    }

    private boolean equals(final AbstractNode node) {
        if (node == null) {
            return false;
        }
        return (this.getId() == (node.getId()));
    }

    @Override
    public int compareTo(final AbstractNode node) {
        // TODO: implement finer compare methods, e.g. taking title and position into account
        if (node == null || node.getName() == null || this.getName() == null) {
            return -1;
        }
        return (this.getName().compareTo(node.getName()));
    }

//    public void setSession(final HttpSession session) {
//        this.session = session;
//    }
//
//    public void setRequest(final HttpServletRequest request) {
//        this.request = request;
//    }
//
//    public HttpSession getSession() {
//        return session;
//    }
//
//    public HttpServletRequest getRequest() {
//        return request;
//    }
    public void setTemplate(final Template template) {
        this.template = template;
    }

    public void createTemplateRelationship(final Template template) {

        // create a relationship to the given template node
        Command createRel = Services.command(CreateRelationshipCommand.class);
        createRel.execute(this, template, RelType.USE_TEMPLATE);

    }

    /**
     * Render a node-specific inline edit view as html
     * 
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    public void renderEditView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        if (getId() == editNodeId.longValue()) {
            renderEditFrame(out, editUrl);
        }
    }

    /**
     * Render an IFRAME element with the given editor URL inside
     *
     * @param out
     * @param editUrl
     */
    protected void renderEditFrame(StringBuilder out, final String editUrl) {
        // create IFRAME with given URL
        out.append("<iframe style=\"border: 1px solid #ccc; background-color: #fff\" src=\"").append(editUrl).append("\" width=\"100%\" height=\"100%\"").append("></iframe>");
    }

    /**
     * Wrapper for toString method
     * @return
     */
    public String getAllProperties() {
        return toString();
    }

    /**
     * Implement standard toString() method
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();

        out.append(getName()).append(" [").append(getId()).append("]: ");

        List<String> props = new LinkedList<String>();

        for (String key : getPropertyKeys()) {

            Object value = getProperty(key);
            String displayValue = "";

            if (value.getClass().isPrimitive()) {
                displayValue = value.toString();
            } else if (value.getClass().isArray()) {

                if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else if (value instanceof char[]) {

                    displayValue = new String((char[]) value);

                } else if (value instanceof double[]) {

                    Double[] values = ArrayUtils.toObject((double[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof float[]) {

                    Float[] values = ArrayUtils.toObject((float[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof short[]) {

                    Short[] values = ArrayUtils.toObject((short[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof long[]) {

                    Long[] values = ArrayUtils.toObject((long[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof int[]) {

                    Integer[] values = ArrayUtils.toObject((int[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof boolean[]) {

                    Boolean[] values = (Boolean[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else {

                    Object[] values = (Object[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
                }


            } else {
                displayValue = value.toString();
            }

            props.add("\"" + key + "\"" + " : " + "\"" + displayValue + "\"");

        }

        out.append("{ ").append(StringUtils.join(props.toArray(), " , ")).append(" }");

        return out.toString();
    }

    /**
     * Write this node as an array of strings
     */
    public String[] toStringArray() {

        List<String> props = new LinkedList<String>();

        for (String key : getPropertyKeys()) {

            Object value = getProperty(key);
            String displayValue = "";

            if (value.getClass().isPrimitive()) {
                displayValue = value.toString();

            } else if (value.getClass().isArray()) {

                if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else if (value instanceof char[]) {

                    displayValue = new String((char[]) value);

                } else if (value instanceof double[]) {

                    Double[] values = ArrayUtils.toObject((double[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof float[]) {

                    Float[] values = ArrayUtils.toObject((float[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof short[]) {

                    Short[] values = ArrayUtils.toObject((short[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof long[]) {

                    Long[] values = ArrayUtils.toObject((long[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof int[]) {

                    Integer[] values = ArrayUtils.toObject((int[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof boolean[]) {

                    Boolean[] values = (Boolean[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else {

                    Object[] values = (Object[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
                }


            } else {
                displayValue = value.toString();
            }

            props.add(displayValue);

        }

        return (String[]) props.toArray(new String[props.size()]);
    }

    /**
     * Render a node-specific view (binary)
     *
     * Should be overwritten by any node which holds binary content
     */
    public void renderDirect(OutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        try {
            if (isVisible(user)) {
                out.write(getName().getBytes());
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not write node name to output stream: {0}", e.getStackTrace());
        }
    }

    /**
     * Get this node's template
     *
     * @return
     */
    public Template getTemplate(final User user) {

        long t0 = System.currentTimeMillis();

        if (this instanceof Template) {
            template = (Template) this;
            return template;
        }

        if (template != null) {
//            long t1 = System.currentTimeMillis();
            logger.log(Level.FINE, "Cached template found");
            return template;
        }

        // TODO: move to command and avoid to use the graph db interface directly
//        Iterable<Node> nodes = Traversal.description().relationships(RelType.HAS_CHILD, Direction.INCOMING).traverse(dbNode).nodes();

        AbstractNode startNode = this;

        while (startNode != null && !(startNode.isRootNode())) {
            List<StructrRelationship> templateRelationships = startNode.getRelationships(RelType.USE_TEMPLATE, Direction.OUTGOING);

            if (templateRelationships != null && !(templateRelationships.isEmpty())) {
                template = (Template) templateRelationships.get(0).getEndNode();
                return template;
            }

            if (template == null) {
                startNode = startNode.getParentNode(user);
                continue;
            }
        }
        long t1 = System.currentTimeMillis();
        logger.log(Level.FINE, "No template found in {0} ms", (t1 - t0));

        return null;

    }

    public boolean hasTemplate(final User user) {
        return (getTemplate(user) != null);
    }

    /**
     * Get type from underlying db node If no type property was found, return
     * info
     */
    public String getType() {
        return (String) getProperty(TYPE_KEY);
    }

    /**
     * Get name from underlying db node
     *
     * If name is null, return node id as fallback
     */
    public String getName() {
        Object nameProperty = getProperty(NAME_KEY);
        if (nameProperty != null) {
            return (String) nameProperty;
        } else {
            return getNodeId().toString();
        }
    }

    /**
     * Get categories
     */
    public String[] getCategories() {
        return (String[]) getProperty(CATEGORIES_KEY);
    }

    /**
     * Get (primary) title
     * 
     * @param locale
     * @return
     */
    public String getTitle(final Locale locale) {
        return (String) getProperty(getTitleKey(locale));
    }

    /*
     * Default, should not be used
     */
    public String getTitle() {
        logger.log(Level.FINE, "Title without locale requested.");
        return getTitle(new Locale("en"));
    }

    public static String getTitleKey(final Locale locale) {
        return TITLE_KEY + "_" + locale;
    }

    /**
     * Get titles from underlying db node
     */
    public List<Title> getTitles() {

        List<Title> titleList = new LinkedList<Title>();

        for (Locale locale : Locale.getAvailableLocales()) {

            String title = (String) getProperty(getTitleKey(locale));

            if (title != null) {
                titleList.add(new Title(locale, title));
            }

        }

        return titleList;
    }

    /**
     * Get id from underlying db
     */
    public long getId() {
        if (isDirty) {
            return -1;
        }
        return dbNode.getId();
    }

    public Long getNodeId() {
        return getId();
    }

    public String getIdString() {
        return Long.toString(getId());
    }

//    public Long getId() {
//        return getId();
//    }
    protected Date getDateProperty(final String key) {
        Object propertyValue = getProperty(key);
        if (propertyValue != null) {
            if (propertyValue instanceof Date) {
                return (Date) propertyValue;
            } else if (propertyValue instanceof Long) {
                return new Date((Long) propertyValue);
            } else if (propertyValue instanceof String) {
                try {

                    // try to parse as a number
                    return new Date(Long.parseLong((String) propertyValue));
                } catch (NumberFormatException nfe) {

                    try {
                        Date date = DateUtils.parseDate(((String) propertyValue), new String[]{"yyyymmdd", "yyyymm", "yyyy"});
                        return date;
                    } catch (ParseException ex2) {
                        logger.log(Level.WARNING, "Could not parse " + propertyValue + " to date", ex2);
                    }

                    logger.log(Level.WARNING, "Can''t parse String {0} to a Date.", propertyValue);
                    return null;
                }
            } else {
                logger.log(Level.WARNING, "Date property is not null, but type is neither Long nor String, returning null");
                return null;
            }
        }
        return null;
    }

    public String getCreatedBy() {
        return (String) getProperty(CREATED_BY_KEY);
    }

    public void setCreatedBy(final String createdBy) {
        setProperty(CREATED_BY_KEY, createdBy);
    }

    public Date getCreatedDate() {
        return getDateProperty(CREATED_DATE_KEY);
    }

    public void setCreatedDate(final Date date) {
        setProperty(CREATED_DATE_KEY, date);
    }

    public Date getLastModifiedDate() {
        return getDateProperty(LAST_MODIFIED_DATE_KEY);
    }

    public void setLastModifiedDate(final Date date) {
        setProperty(LAST_MODIFIED_DATE_KEY, date);
    }

    public Date getVisibilityStartDate() {
        return getDateProperty(VISIBILITY_START_DATE_KEY);
    }

    public void setVisibilityStartDate(final Date date) {
        setProperty(VISIBILITY_START_DATE_KEY, date);
    }

    public Date getVisibilityEndDate() {
        return getDateProperty(VISIBILITY_END_DATE_KEY);
    }

    public void setVisibilityEndDate(final Date date) {
        setProperty(VISIBILITY_END_DATE_KEY, date);
    }

    public Long getPosition() {

        Object p = getProperty(POSITION_KEY);
        Long pos;

        if (p != null) {

            if (p instanceof Long) {
                return (Long) p;
            } else if (p instanceof Integer) {

                try {
                    pos = Long.parseLong(p.toString());
                    // convert old String-based position property
                    setPosition(pos);
                } catch (NumberFormatException e) {
                    pos = getId();
                    return pos;
                }

            } else if (p instanceof String) {
                try {
                    pos = Long.parseLong(((String) p));
                    // convert old String-based position property
                    setPosition(pos);
                } catch (NumberFormatException e) {
                    pos = getId();
                    return pos;
                }
            } else {
                logger.log(Level.SEVERE, "Position property not stored as Integer or String: {0}", p.getClass().getName());
            }

        }
        return getId();

    }

    public void setPosition(final Long position) {
        setProperty(POSITION_KEY, position);
    }

    public boolean isPublic() {
        return getBooleanProperty(PUBLIC_KEY);
    }

    public boolean getPublic() {
        return getBooleanProperty(PUBLIC_KEY);
    }

    public void setPublic(final boolean publicFlag) {
        setProperty(PUBLIC_KEY, publicFlag);
    }

    public boolean isVisibleToAuthenticatedUsers() {
        return getBooleanProperty(VISIBLE_TO_AUTHENTICATED_USERS_KEY);
    }

    public boolean getVisibleToAuthenticatedUsers() {
        return getBooleanProperty(VISIBLE_TO_AUTHENTICATED_USERS_KEY);
    }

    public void setVisibleToAuthenticatedUsers(final boolean flag) {
        setProperty(VISIBLE_TO_AUTHENTICATED_USERS_KEY, flag);
    }

    public boolean isHidden() {
        return getHidden();
    }

    public boolean getHidden() {
        return getBooleanProperty(HIDDEN_KEY);
    }

    public void setHidden(final boolean hidden) {
        setProperty(HIDDEN_KEY, hidden);
    }

    public boolean isDeleted() {
        return getDeleted();
    }

    public boolean getDeleted() {
        return getBooleanProperty(DELETED_KEY);
    }

    public void setDeleted(final boolean deleted) {
        setProperty(DELETED_KEY, deleted);
    }

    public void setType(final String type) {
        setProperty(TYPE_KEY, type);
    }

    public void setName(final String name) {
        setProperty(NAME_KEY, name);
    }

    public void setCategories(final String[] categories) {
        setProperty(CATEGORIES_KEY, categories);
    }

    public void setTitle(final String title) {
        setProperty(TITLE_KEY, title);
    }

    /**
     * Multiple titles (one for each language)
     * 
     * @param title
     */
    public void setTitles(final String[] titles) {
        setProperty(TITLES_KEY, titles);
    }

    public void setId(final Long id) {
        //setProperty(NODE_ID_KEY, id);
        // not allowed
    }

    public void setNodeId(final Long id) {
        //setProperty(NODE_ID_KEY, id);
        // not allowed
    }

    public Map<String, Object> getPropertyMap() {
        return properties;
    }

    public Iterable<String> getPropertyKeys() {
        return dbNode.getPropertyKeys();
    }

    public Object getProperty(final String key) {

        if (isDirty) {
            return properties.get(key);
        }

        if (key != null && dbNode.hasProperty(key)) {
            return dbNode.getProperty(key);
        } else {
            return null;
        }
    }

    public String getStringProperty(final String key) {
        Object propertyValue = getProperty(key);
        String result = null;
        if (propertyValue == null) {
            return null;
        }
        if (propertyValue instanceof String) {
            result = ((String) propertyValue);
        }
        return result;
    }

    public List<String> getStringListProperty(final String key) {
        Object propertyValue = getProperty(key);
        List<String> result = new LinkedList<String>();
        if (propertyValue == null) {
            return null;
        }
        if (propertyValue instanceof String) {

            // Split by carriage return / line feed
            String[] values = StringUtils.split(((String) propertyValue), "\r\n");
            result = Arrays.asList(values);
        } else if (propertyValue instanceof String[]) {

            String[] values = (String[]) propertyValue;
            result = Arrays.asList(values);
        }
        return result;
    }

    public String getStringArrayPropertyAsString(final String key) {
        Object propertyValue = getProperty(key);
        StringBuilder result = new StringBuilder();
        if (propertyValue instanceof String[]) {
            int i = 0;
            String[] values = (String[]) propertyValue;
            for (String value : values) {
                result.append(value);
                if (i < values.length - 1) {
                    result.append("\r\n");
                }
            }
        }
        return result.toString();
    }

    public int getIntProperty(final String key) {
        Object propertyValue = getProperty(key);
        Integer result = null;
        if (propertyValue == null) {
            return 0;
        }
        if (propertyValue instanceof Integer) {
            result = ((Integer) propertyValue).intValue();
        } else if (propertyValue instanceof String) {
            result = Integer.parseInt(((String) propertyValue));
        }
        return result;
    }

    public long getLongProperty(final String key) {
        Object propertyValue = getProperty(key);
        Long result = null;
        if (propertyValue == null) {
            return 0L;
        }
        if (propertyValue instanceof Long) {
            result = ((Long) propertyValue).longValue();
        } else if (propertyValue instanceof String) {
            result = Long.parseLong(((String) propertyValue));
        }
        return result;
    }

    public double getDoubleProperty(final String key) {
        Object propertyValue = getProperty(key);
        Double result = null;
        if (propertyValue == null) {
            return 0.0d;
        }
        if (propertyValue instanceof Double) {
            result = ((Double) propertyValue).doubleValue();
        } else if (propertyValue instanceof String) {
            result = Double.parseDouble(((String) propertyValue));
        }
        return result;
    }

    public boolean getBooleanProperty(final String key) {
        Object propertyValue = getProperty(key);
        Boolean result = false;
        if (propertyValue == null) {
            return Boolean.FALSE;
        }
        if (propertyValue instanceof Boolean) {
            result = ((Boolean) propertyValue).booleanValue();
        } else if (propertyValue instanceof String) {
            result = Boolean.parseBoolean(((String) propertyValue));
        }
        return result;
    }

    /**
     * Set a property in database backend without updating index
     *
     * Set property only if value has changed
     * 
     * @param key
     * @param value
     */
    public void setProperty(final String key, final Object value) {
        setProperty(key, value, updateIndexDefault);
    }

    /**
     * Split String value and set as String[] property in database backend
     *
     * @param key
     * @param stringList
     *
     */
    public void setPropertyAsStringArray(final String key, final String value) {
        String[] values = StringUtils.split(((String) value), "\r\n");
        setProperty(key, values, updateIndexDefault);
    }

    /**
     * Set a property in database backend
     *
     * Set property only if value has changed
     *
     * Update index only if updateIndex is true
     *
     * @param key
     * @param value
     * @param updateIndex
     */
    public void setProperty(final String key, final Object value, final boolean updateIndex) {

        if (key == null) {
            logger.log(Level.SEVERE, "Tried to set property will null key (action was denied)");
            return;
        }


        if (isDirty) {

            // Don't write directly to database, but store property values
            // in a map for later use
            properties.put(key, value);

        } else {

            // Commit value directly to database

            Object oldValue = getProperty(key);

            // don't make any changes if
            // - old and new value both are null
            // - old and new value are not null but equal
            if ((value == null && oldValue == null)
                    || (value != null && oldValue != null && value.equals(oldValue))) {
                return;
            }

            Command transactionCommand = Services.command(TransactionCommand.class);
            transactionCommand.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {

                    // save space
                    if (value == null) {
                        dbNode.removeProperty(key);
                    } else {

                        // Setting last modified date explicetely is not allowed
                        if (!key.equals(AbstractNode.LAST_MODIFIED_DATE_KEY)) {

                            if (value instanceof Date) {
                                dbNode.setProperty(key, ((Date) value).getTime());
                            } else {
                                dbNode.setProperty(key, value);

                                // set last modified date if not already happened
                                dbNode.setProperty(AbstractNode.LAST_MODIFIED_DATE_KEY, (new Date()).getTime());
                            }
                        } else {
                            logger.log(Level.FINE, "Tried to set lastModifiedDate explicitely (action was denied)");
                        }
                    }

                    // Don't automatically update index
                    // TODO: Implement something really fast to keep the index automatically in sync
                    if (updateIndex && dbNode.hasProperty(key)) {
                        Services.command(IndexNodeCommand.class).execute(getId(), key);
                    }

                    return null;
                }
            });

        }
    }

    /**
     * Discard changes and overwrite the properties map with the values
     * from database
     */
    public void discard() {
        // TODO: Implement the full pattern with commit(), discard(), init() etc.
    }

    /**
     * Commit unsaved property values to the database node.
     */
    public void commit(final User user) {

        isDirty = false;

        // Create an outer transaction to combine any inner neo4j transactions
        // to one single transaction
        Command transactionCommand = Services.command(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command createNode = Services.command(CreateNodeCommand.class);
                AbstractNode s = (AbstractNode) createNode.execute(user);

                init(s);

                Set<String> keys = properties.keySet();
                for (String key : keys) {
                    Object value = properties.get(key);
                    if (key != null && value != null) {
                        setProperty(key, value, false); // Don't update index now!
                    }
                }
                return null;
            }
        });

    }

    /**
     * Return database node
     *
     * @return
     */
    public Node getNode() {
        return dbNode;
    }

    /**
     * Render a minimal html header
     *
     * @param out
     */
    protected void renderHeader(StringBuilder out) {
        out.append("<html><head><title>").append(getName()).append(" (Domain)</title></head><body>");
    }

    /**
     * Render a minimal html footer
     *
     * @param out
     */
    protected void renderFooter(StringBuilder out) {
        out.append("</body></html>");
    }
    /*
    @Override
    public int compareTo(AbstractNode otherNode) {
    return this.getPosition().compareTo(otherNode.getPosition());
    }
     */

    /**
     * Get path relative to given node
     * 
     * @param node
     * @return
     */
    public String getNodePath(final User user, final AbstractNode node) {

        // clicked node as reference
        String refPath = node.getNodePath(user);

        // currently rendered node, the link target
        String thisPath = this.getNodePath(user);

        String[] refParts = refPath.split("/");
        String[] thisParts = thisPath.split("/");

        int level = refParts.length - thisParts.length;

        if (level == 0) {
            // paths are identical, return last part
            return thisParts[thisParts.length - 1];

        } else if (level < 0) {
            // link down
            return thisPath.substring(refPath.length());

        } else {
            // link up
            int i = 0;
            String ret = "";
            do {
                ret = ret.concat("../");
            } while (++i < level);

            return ret.concat(thisParts[thisParts.length - 1]);

        }

    }

    /**
     * Assemble path for given node.
     *
     * This is an inverse method of @getNodeByIdOrPath.
     *
     * @param node
     * @param renderMode
     * @return
     *//*
    public String getNodePath(final AbstractNode node, final Enum renderMode) {

    Command nodeFactory = Services.command(NodeFactoryCommand.class);
    AbstractNode n = (AbstractNode) nodeFactory.execute(node);
    return n.getNodePath();
    }*/

    /**
     * Assemble absolute path for given node.
     *
     * @return
     */
    public String getNodePath(final User user) {

        String path = "";

        // get actual database node
        AbstractNode node = this;

        // create bean node
//        Command nodeFactory = Services.command(NodeFactoryCommand.class);
//        AbstractNode n = (AbstractNode) nodeFactory.execute(node);

        // stop at root node
        while (node != null && node.getId() > 0) {

            path = node.getName() + (!("".equals(path)) ? "/" + path : "");

            node = node.getParentNode(user);

            // check parent nodes
//            Relationship r = node.getSingleRelationship(RelType.HAS_CHILD, Direction.INCOMING);
//            if (r != null) {
//                node = r.getStartNode();
//                n = (AbstractNode) nodeFactory.execute(node);
//            }

        }

        return "/".concat(path); // add leading slash, because we always include the root node
    }

    /**
     * Assemble absolute path for given node.
     *
     * @return
     */
    public String getNodeXPath(final User user) {

        String xpath = "";

        // get actual database node
        AbstractNode node = this;

        // create bean node
//        Command nodeFactory = Services.command(NodeFactoryCommand.class);
//        AbstractNode n = (AbstractNode) nodeFactory.execute(node);

        // stop at root node
        while (node != null && node.getId() > 0) {

            xpath = node.getType() + "[@name='" + node.getName() + "']" + (!("".equals(xpath)) ? "/" + xpath : "");

            // check parent nodes
            node = node.getParentNode(user);

        }

        return "/".concat(xpath); // add leading slash, because we always include the root node
    }

    public String getNodeURL(final User user, final String contextPath) {
        return getNodeURL(user, RenderMode.PUBLIC, contextPath);
    }

    /**
     * Assemble URL for this node.
     *
     * This is an inverse method of @getNodeByIdOrPath.
     *
     * @param renderMode
     * @param contextPath
     * @return
     */
    public String getNodeURL(final User user, final Enum renderMode, final String contextPath) {

        String domain = "";
        String site = "";
        String path = "";


        if (RenderMode.PUBLIC.equals(renderMode)) {

            // create bean node
            Command nodeFactory = Services.command(NodeFactoryCommand.class);
            AbstractNode node = (AbstractNode) nodeFactory.execute(this);

            // TODO: remove references to db nodes
            //Node node = n.getNode();

            // stop at root node
            while (node != null && node.getId() > 0) {

                String urlPart = node.getUrlPart();
                if (urlPart != null) {
                    if (urlPart.startsWith("http://")) {
                        site = urlPart;
                    } else if (urlPart.endsWith("/")) {
                        domain = urlPart;
                    } else {
                        path = node.getUrlPart() + (!("".equals(path)) ? "/" + path : "");
                    }
                }

                // check parent nodes
                node = node.getParentNode(user);
//                StructrRelationship r = node.getRelationships(RelType.HAS_CHILD, Direction.INCOMING).get(0);
//                if (r != null) {
//                    node = r.getStartNode();
//                }
            }

            return site + (site != null ? "." : "") + domain + path;

        } else if (RenderMode.LOCAL.equals(renderMode)) {
            // assemble relative URL following the pattern
            // /<context-url>/view.htm?nodeId=<path>
            // TODO: move this to a better place
            // TODO: improve handling for renderMode
            String localUrl = contextPath.concat(getNodePath(user)).concat("&renderMode=local");
            return localUrl;

        } else {
            // TODO implement other modes
            return null;
        }
    }

    /**
     * Default: Return this node's name
     * 
     * @param user
     * @param renderMode
     * @param contextPath
     * @return
     */
    public String getUrlPart() {
        return getName();
    }

    /**
     * Return null mime type. Method has to be overwritten,
     * returning real mime type
     */
    public String getContentType() {
        return null;
    }

    /**
     * Test: Evaluate BeanShell script in this text node.
     *
     * @return the output
     */
    public String evaluate() {
        return ("");
    }

    /**
     * Return true if this node has a relationship of given type and direction.
     *
     * @param type
     * @param dir
     * @return
     */
    public boolean hasRelationship(final RelType type, final Direction dir) {

        List<StructrRelationship> rels = this.getRelationships(type, dir);
        return (rels != null && !(rels.isEmpty()));
    }

    /**
     * Return the (cached) incoming relationship between this node and the
     * given user which holds the security information.
     *
     * @param user
     * @return incoming security relationship
     */
    public StructrRelationship getSecurityRelationship(final User user) {

        long userId = user.getId();

        if (securityRelationships == null) {
            securityRelationships = new HashMap<Long, StructrRelationship>();
        }

        if (!(securityRelationships.containsKey(userId))) {
            populateSecurityRelationshipCacheMap();
        }

        return securityRelationships.get(userId);

    }

    /**
     * Populate the security relationship cache map
     */
    private void populateSecurityRelationshipCacheMap() {

        if (securityRelationships == null) {
            securityRelationships = new HashMap<Long, StructrRelationship>();
        }
        // Fill cache map
        for (StructrRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {
            securityRelationships.put(r.getStartNode().getId(), r);
        }

    }

    /**
     * Return all relationships of given type and direction
     *
     * @return list with relationships
     */
    public List<StructrRelationship> getRelationships(RelType type, Direction dir) {
        return (List<StructrRelationship>) Services.command(NodeRelationshipsCommand.class).execute(this, type, dir);
    }

    /**
     * Return true if node is the root node
     * 
     * @return
     */
    public boolean isRootNode() {
        return getId() == 0;
    }

    /**
     * Return true if this node has child nodes
     * 
     * @return
     */
    public boolean hasChildren() {
        return (hasRelationship(RelType.HAS_CHILD, Direction.OUTGOING)
                || hasRelationship(RelType.LINK, Direction.OUTGOING));
    }

    /**
     * Return true if this node has child nodes visible for current user
     *
     * @return
     */
    public boolean hasChildren(final User user) {
        List<StructrRelationship> childRels = getOutgoingChildRelationships();
        List<StructrRelationship> linkRels = getOutgoingLinkRelationships();
        return (linkRels != null && !(linkRels.isEmpty())
                && childRels != null && !(childRels.isEmpty()));
//        return (hasRelationship(RelType.HAS_CHILD, Direction.OUTGOING)
//                || hasRelationship(RelType.LINK, Direction.OUTGOING));
    }

    /**
     * Return unordered list of all direct child nodes (no recursion)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getDirectChildNodes(final User user) {

        return getDirectChildren(RelType.HAS_CHILD, user);

    }

    /**
     * Return the first parent node found.
     * 
     * @return
     */
    public AbstractNode getParentNode(final User user) {
        List<AbstractNode> parentNodes = getParentNodes(user);
        if (parentNodes != null && !(parentNodes.isEmpty())) {
            return parentNodes.get(0);
        } else {
            return null;
        }
    }

    /**
     * Return sibling nodes. Follows the HAS_CHILD relationship
     *
     * @return
     */
    public List<AbstractNode> getSiblingNodes(final User user) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        AbstractNode parentNode = getParentNode(user);

        if (parentNode != null) {


            Command nodeFactory = Services.command(NodeFactoryCommand.class);

            Command relsCommand = Services.command(NodeRelationshipsCommand.class);
            List<StructrRelationship> rels = (List<StructrRelationship>) relsCommand.execute(parentNode, RelType.HAS_CHILD, Direction.OUTGOING);

            for (StructrRelationship r : rels) {

                AbstractNode s = (AbstractNode) nodeFactory.execute(r.getEndNode());
                if (s.readAllowed(user)) {
                    nodes.add(s);
                }

            }
        }
        return nodes;

    }

    /**
     * Return all ancestor nodes. Follows the INCOMING HAS_CHILD relationship
     * and stops at the root node.
     *
     * @return
     */
    public List<AbstractNode> getAncestorNodes(final User user) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        Command nodeFactory = Services.command(NodeFactoryCommand.class);
        List<StructrRelationship> rels = getIncomingChildRelationships();

        for (StructrRelationship r : rels) {

            AbstractNode s = (AbstractNode) nodeFactory.execute(r.getStartNode());
            if (s.readAllowed(user)) {
                nodes.add(s);
            }

        }

        return nodes;

    }

    /**
     * Return parent nodes. Follows the INCOMING HAS_CHILD relationship
     * 
     * @return
     */
    public List<AbstractNode> getParentNodes(final User user) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        Command nodeFactory = Services.command(NodeFactoryCommand.class);
        List<StructrRelationship> rels = getIncomingChildRelationships();

        for (StructrRelationship r : rels) {

            AbstractNode s = (AbstractNode) nodeFactory.execute(r.getStartNode());
            if (s.readAllowed(user)) {
                nodes.add(s);
            }

        }

        return nodes;

    }

    /**
     * Cached list of all relationships
     *
     * @return
     */
    public List<StructrRelationship> getRelationships() {

        if (allRelationships == null) {
            allRelationships = getRelationships(null, Direction.BOTH);
        }
        return allRelationships;
    }

    /**
     * Get all relationships of given direction
     *
     * @return
     */
    public List<StructrRelationship> getRelationships(Direction dir) {
        return getRelationships(null, dir);
    }

    /**
     * Cached list of incoming relationships
     *
     * @return
     */
    public List<StructrRelationship> getIncomingRelationships() {

        if (incomingRelationships == null) {
            incomingRelationships = getRelationships(null, Direction.INCOMING);
        }
        return incomingRelationships;
    }

    /**
     * Cached list of outgoing relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingRelationships() {

        if (outgoingRelationships == null) {
            outgoingRelationships = getRelationships(null, Direction.OUTGOING);
        }
        return outgoingRelationships;
    }

    /**
     * Cached list of incoming link relationships
     *
     * @return
     */
    public List<StructrRelationship> getIncomingLinkRelationships() {

        if (incomingLinkRelationships == null) {
            incomingLinkRelationships = getRelationships(RelType.LINK, Direction.INCOMING);
        }
        return incomingLinkRelationships;
    }

    /**
     * Cached list of outgoing data relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingDataRelationships() {

        if (outgoingDataRelationships == null) {
            outgoingDataRelationships = getRelationships(RelType.DATA, Direction.OUTGOING);
        }
        return outgoingDataRelationships;
    }

    /**
     * Cached list of outgoing link relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingLinkRelationships() {

        if (outgoingLinkRelationships == null) {
            outgoingLinkRelationships = getRelationships(RelType.LINK, Direction.OUTGOING);
        }
        return outgoingLinkRelationships;
    }

    /**
     * Cached list of incoming child relationships
     *
     * @return
     */
    public List<StructrRelationship> getIncomingChildRelationships() {

        if (incomingChildRelationships == null) {
            incomingChildRelationships = getRelationships(RelType.HAS_CHILD, Direction.INCOMING);
        }
        return incomingChildRelationships;
    }

    /**
     * Cached list of outgoing child relationships
     *
     * @return
     */
    public List<StructrRelationship> getOutgoingChildRelationships() {

        if (outgoingChildRelationships == null) {
            outgoingChildRelationships = getRelationships(RelType.HAS_CHILD, Direction.OUTGOING);
        }
        return outgoingChildRelationships;
    }

    /**
     * Return unordered list of all directly linked nodes (no recursion)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getLinkedNodes(final User user) {

        return getDirectChildren(RelType.LINK, user);

    }

    /**
     * Return ordered list of all directly linked nodes (no recursion)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getSortedLinkedNodes(final User user) {

        return getSortedDirectChildren(RelType.LINK, user);

    }

    /**
     * Return unordered list of all child nodes (recursively)
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getAllChildren(final User user) {
        return getAllChildren(null, user);
    }

    /**
     * Return unordered list of all direct child nodes (no recursion)
     * with given relationship type
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getDirectChildren(final Enum relType, final User user) {
        return getDirectChildren(relType, null, user);
    }

    /**
     * Return ordered list of all direct child nodes (no recursion)
     * with given relationship type
     *
     * @return list with structr nodes
     */
    public List<AbstractNode> getSortedDirectChildren(final Enum relType, final User user) {
        List<AbstractNode> nodes = getDirectChildren(relType, null, user);
        Collections.sort(nodes);
        return nodes;
    }

    /**
     * Return unordered list of all direct child nodes (no recursion)
     * with given relationship type and given node type.
     *
     * Given user must have read access.
     *
     * @return list with structr nodes
     */
    private List<AbstractNode> getDirectChildren(final Enum relType, final String nodeType, final User user) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        Command nodeFactory = null;
        if (relType.equals(RelType.LINK)) {
            nodeFactory = Services.command(LinkNodeFactoryCommand.class);
        } else {
            nodeFactory = Services.command(NodeFactoryCommand.class);
        }

        Command relsCommand = Services.command(NodeRelationshipsCommand.class);
        List<StructrRelationship> rels = (List<StructrRelationship>) relsCommand.execute(this, relType, Direction.OUTGOING);

        for (StructrRelationship r : rels) {

            AbstractNode s = (AbstractNode) nodeFactory.execute(r.getEndNode());

            if (s.readAllowed(user) && (nodeType == null || nodeType.equals(s.getType()))) {
                nodes.add(s);
            }

        }
        return nodes;
    }

    /**
     * Get child nodes and sort them before returning
     *
     * @return
     */
    public List<AbstractNode> getSortedDirectChildNodes(final User user) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();
        nodes.addAll(getDirectChildNodes(user));

        // sort by position
        Collections.sort(nodes, new Comparator<AbstractNode>() {

            @Override
            public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
            }
        });
        return nodes;
    }

    /**
     * Get direct child nodes, link nodes, and sort them before returning
     * 
     * @return
     */
    public List<AbstractNode> getSortedDirectChildAndLinkNodes(final User user) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();
        nodes.addAll(getDirectChildNodes(user));

        // get linked child nodes
        nodes.addAll(getLinkedNodes(user));

        // sort by position
        Collections.sort(nodes, new Comparator<AbstractNode>() {

            @Override
            public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
            }
        });
        return nodes;
    }

    /**
     * Get menu items and sort them before returning.
     *
     * @return
     */
    public List<AbstractNode> getSortedMenuItems(final User user) {

        List<AbstractNode> menuItems = new LinkedList<AbstractNode>();

        // add direct children of type MenuItem
        menuItems.addAll(getDirectChildren(RelType.HAS_CHILD, "MenuItem", user));

        // add linked children, f.e. direct links to pages
        menuItems.addAll(getDirectChildren(RelType.LINK, user));

        // sort by position
        Collections.sort(menuItems, new Comparator<AbstractNode>() {

            @Override
            public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
            }
        });
        return menuItems;
    }

    /**
     * Return unordered list of all child nodes (recursively)
     * with given relationship type and given node type.
     *
     * Given user must have read access.
     *
     * @param nodeType node type filter, can be null
     * @param user
     * @return list with structr nodes
     */
    protected List<AbstractNode> getAllChildren(final String nodeType, final User user) {

        List<AbstractNode> nodes = new LinkedList<AbstractNode>();

        Command findNode = Services.command(FindNodeCommand.class);
        List<AbstractNode> result = (List<AbstractNode>) findNode.execute(user, this);

        for (AbstractNode s : result) {

            if (s.readAllowed(user) && (nodeType == null || nodeType.equals(s.getType()))) {
                nodes.add(s);
            }

        }
        return nodes;
    }

    /**
     * Check visibility of given node, used for rendering in view mode
     *
     * @return
     */
    public boolean isVisible(final User user) {

        if (user instanceof SuperUser) {
            // Super user may always see it
            return true;
        }

        // check hidden flag (see STRUCTR-12)
        if (isHidden()) {
            return false;
        }

        boolean visibleByTime = false;

        // check visibility period of time (see STRUCTR-13)
        Date visStartDate = getVisibilityStartDate();

        long effectiveStartDate = 0L;
        Date createdDate = getCreatedDate();
        if (createdDate != null) {
            effectiveStartDate = Math.max(createdDate.getTime(), 0L);
        }

        // if no start date for visibility is given,
        // take the maximum of 0 and creation date.
        visStartDate = (visStartDate == null ? new Date(effectiveStartDate) : visStartDate);

        // if no end date for visibility is given,
        // take the Long.MAX_VALUE
        Date visEndDate = getVisibilityEndDate();
        visEndDate = (visEndDate == null ? new Date(Long.MAX_VALUE) : visEndDate);

        Date now = new Date();

        visibleByTime = (now.after(visStartDate) && now.before(visEndDate));


        if (user == null) {

            // No logged-in user

            if (isPublic()) {
                return visibleByTime;
            } else {
                return false;
            }

        } else {

            // Logged-in users

            if (isVisibleToAuthenticatedUsers()) {
                return visibleByTime;
            } else {
                return false;
            }

        }

    }

    /**
     * Return true if user has the given permission
     * 
     * @param permission
     * @param user
     * @return
     */
    private boolean hasPermission(final String permission, final User user) {

        // just in case ...
        if (user == null || permission == null) {
            return false;
        }

        // superuser
        if (user instanceof SuperUser) {
            return true;
        }

        // user has full control over his/her own user node
        if (this.equals(user)) {
            return true;
        }


        StructrRelationship r = null;

        r = getSecurityRelationship(user);

        if (r != null && r.isAllowed(permission)) {
            return true;
        }

        return false;
    }

    /**
     * Check if given node may be read by current user.
     *
     * @return
     */
    public boolean readAllowed(final User user) {

        // Check global settings first
        if (isVisible(user)) {
            return true;
        }

        // Then check per-user permissions
        return hasPermission(StructrRelationship.READ_KEY, user);
    }

    /**
     * Check if given node may see the navigation tree
     *
     * @return
     */
    public boolean showTreeAllowed(final User user) {
        return hasPermission(StructrRelationship.SHOW_TREE_KEY, user);
    }

    /**
     * Check if given node may be written by current user.
     *
     * @return
     */
    public boolean writeAllowed(final User user) {
        return hasPermission(StructrRelationship.WRITE_KEY, user);
    }

    /**
     * Check if given user may create new sub nodes.
     *
     * @return
     */
    public boolean createSubnodeAllowed(final User user) {
        return hasPermission(StructrRelationship.CREATE_SUBNODE_KEY, user);
    }

    /**
     * Check if given user may delete this node
     *
     * @return
     */
    public boolean deleteNodeAllowed(final User user) {
        return hasPermission(StructrRelationship.DELETE_NODE_KEY, user);
    }

    /**
     * Check if given user may add new relationships to this node
     *
     * @return
     */
    public boolean addRelationshipAllowed(final User user) {
        return hasPermission(StructrRelationship.ADD_RELATIONSHIP_KEY, user);
    }

    /**
     * Check if given user may edit (set) properties of this node
     *
     * @return
     */
    public boolean editPropertiesAllowed(final User user) {
        return hasPermission(StructrRelationship.EDIT_PROPERTIES_KEY, user);
    }

    /**
     * Check if given user may remove relationships to this node
     *
     * @return
     */
    public boolean removeRelationshipAllowed(final User user) {
        return hasPermission(StructrRelationship.REMOVE_RELATIONSHIP_KEY, user);
    }

    /**
     * Check if access of given node may be controlled by current user.
     *
     * @return
     */
    public boolean accessControlAllowed(final User user) {

        // just in case ...
        if (user == null) {
            return false;
        }

        // superuser
        if (user instanceof SuperUser) {
            return true;
        }

        // node itself
        if (this.equals(user)) {
            return true;
        }

        StructrRelationship r = null;


        // owner has always access control
        if (user.equals(getOwnerNode())) {
            return true;
        }

        r = getSecurityRelationship(user);

        if (r != null && r.isAllowed(StructrRelationship.ACCESS_CONTROL_KEY)) {
            return true;
        }

        return false;
    }

    /**
     * Return owner node
     *
     * @return
     */
    public User getOwnerNode() {

        // check any security relationships
        for (StructrRelationship s : getRelationships(RelType.OWNS, Direction.INCOMING)) {

            // check security properties
            return (User) s.getStartNode();

        }
        return null;
    }

    /**
     * Return owner
     *
     * @return
     */
    public String getOwner() {
        User user = getOwnerNode();
        return (user != null ? user.getRealName() + " (" + user.getName() + ")" : null);
    }

    /**
     * Return a list with the connected principals (user, group, role)
     * @return
     */
    public List<AbstractNode> getSecurityPrincipals() {

        List<AbstractNode> principalList = new LinkedList<AbstractNode>();

        // check any security relationships
        for (StructrRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {

            // check security properties
            AbstractNode principalNode = r.getEndNode();

            principalList.add(principalNode);

        }
        return principalList;
    }

    /**
     * Replace $(key) by the content rendered by the subnode with name "key"
     *
     * @param content
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    protected void replaceBySubnodes(StringBuilder content, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user) {

//        List<AbstractNode> subnodes = getSortedDirectChildAndLinkNodes(user);
        List<AbstractNode> callingNodeSubnodes = null;
        List<AbstractNode> callingNodeSubnodesAndLinkedNodes = null;

        template = startNode.getTemplate(user);
        AbstractNode callingNode = null;
        if (template != null) {

            callingNode = template.getCallingNode();
            if (callingNode != null) {
                callingNodeSubnodesAndLinkedNodes = callingNode.getSortedDirectChildAndLinkNodes(user);
                callingNodeSubnodes = callingNode.getSortedDirectChildNodes(user);
            }
        }

        Command findNode = Services.command(FindNodeCommand.class);

        int start = content.indexOf(NODE_KEY_PREFIX);
        while (start > -1) {

            int end = content.indexOf(NODE_KEY_SUFFIX, start + NODE_KEY_PREFIX.length());

            if (end < 0) {
                logger.log(Level.WARNING, "Node key suffix {0} not found in template {1}", new Object[]{NODE_KEY_SUFFIX, template.getName()});
                break;
            }

            String key = content.substring(start + NODE_KEY_PREFIX.length(), end);

            int indexOfComma = key.indexOf(",");

            String templateKey = null;
            if (indexOfComma > 0) {
                String[] splitted = StringUtils.split(key, ",");
                key = splitted[0];
                templateKey = splitted[1];
            }

            Template customTemplate = null;
            if (templateKey != null && !(templateKey.isEmpty())) {
//                List<AbstractNode> templates = (List<AbstractNode>) findNode.execute(user, this, templateKey);
                customTemplate = (Template) findNode.execute(user, this, new XPath(templateKey));
//                if (templates != null && templates.size() == 1) {
//                    customTemplate = (Template) templates.get(0);
//                }
            }


            StringBuilder replacement = new StringBuilder();

            if (callingNode != null && key.equals(CALLING_NODE_SUBNODES_KEY)) {

                // render subnodes in correct order
                for (AbstractNode s : callingNodeSubnodes) {

                    // propagate request and template
//                    s.setRequest(request);
                    s.renderView(replacement, startNode, editUrl, editNodeId, user);
                }

            } else if (callingNode != null && key.equals(CALLING_NODE_SUBNODES_AND_LINKED_NODES_KEY)) {

                // render subnodes in correct order
                for (AbstractNode s : callingNodeSubnodesAndLinkedNodes) {

                    // propagate request and template
//                    s.setRequest(request);
                    s.renderView(replacement, startNode, editUrl, editNodeId, user);
                }

            } else { //if (key.startsWith("/") || key.startsWith("count(")) {
                // use XPath notation


                // search relative to calling node
                //List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(user, callingNode, new XPath(key));

//                Object result = findNode.execute(user, this, new XPath(key));
                Object result = findNode.execute(user, this, key);

                if (result instanceof List) {

                    // get referenced nodes relative to the template
                    List<AbstractNode> nodes = (List<AbstractNode>) result;

                    if (nodes != null) {
                        for (AbstractNode s : nodes) {

                            if (customTemplate != null) {
                                s.setTemplate(customTemplate);
                            }

                            // propagate request
//                            s.setRequest(getRequest());
                            s.renderView(replacement, startNode, editUrl, editNodeId, user);
                        }
                    }
                } else if (result instanceof AbstractNode) {

                    AbstractNode s = (AbstractNode) result;

                    if (customTemplate != null) {
                        s.setTemplate(customTemplate);
                    }

                    // propagate request
//                    s.setRequest(getRequest());
                    s.renderView(replacement, startNode, editUrl, editNodeId, user);

                } else {
                    replacement.append(result);

                }

            }
//            else {
//
//                // subnodes of this object
//                for (AbstractNode s : subnodes) {
//
//                    if (key.equals(s.getName())) {
//
//                        // propagate request
//                        s.setRequest(getRequest());
//                        s.renderView(replacement, startNode, editUrl, editNodeId, user);
//
//                    }
//                }
//            }
            String replaceBy = replacement.toString();

            content.replace(start, end
                    + NODE_KEY_SUFFIX.length(), replaceBy);
            // avoid replacing in the replacement again
            start = content.indexOf(NODE_KEY_PREFIX, start + replaceBy.length() + 1);
        }

    }

    /**
     * Replace ${key} by the value of calling node's property with the name "key".
     *
     * Alternatively, propertyName can be
     *
     * @param content
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
//    protected void replaceByPropertyValues(StringBuilder content, final AbstractNode startNode, final String editUrl, final Long editNodeId) {
//        // start with first occurrence of key prefix
//        int start = content.indexOf(keyPrefix);
//
//        while (start > -1) {
//
//            int end = content.indexOf(keySuffix, start + keyPrefix.length());
//            String key = content.substring(start + keyPrefix.length(), end);
//
//            StringBuilder replacement = new StringBuilder();
//
//            // special placeholder for calling node's child nodes
//            if (key.equals(CALLING_NODE_KEY)) {
//
//                List<AbstractNode> subnodes = callingNode.getSortedDirectChildAndLinkNodes();
//
//                // render subnodes in correct order
//                for (AbstractNode s : subnodes) {
//
//                    // propagate request
//                    s.setRequest(getRequest());
//                    s.renderView(replacement, startNode, editUrl, editNodeId);
//                }
//
//            } else if (callingNode.getNode() != null && callingNode.getNode().hasProperty(key)) {
//                // then, look for a property with name=key
//                replacement.append(callingNode.getProperty(key));
//            }
//            // moved to replaceBySubnodes
////            } else {
////
////                // use XPath notation
////                Command findNode = Services.command(FindNodeCommand.class);
////
////                // search relative to calling node
////                //List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(user, callingNode, new XPath(key));
////
////                // get referenced nodes relative to the template
////                List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(user, this, new XPath(key));
////
////                if (nodes != null) {
////                    for (AbstractNode s : nodes) {
////                        // propagate request
////                        s.setRequest(getRequest());
////                        s.renderView(replacement, startNode, editUrl, editNodeId);
////                    }
////                }
////
////            }
//            String replaceBy = replacement.toString();
//            content.replace(start, end + keySuffix.length(), replaceBy);
//            // avoid replacing in the replacement again
//            start = content.indexOf(keyPrefix, start + replaceBy.length() + 1);
//        }
//    }
    protected void replaceByFreeMarker(final String templateString, Writer out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user) {

        Configuration cfg = new Configuration();

        // TODO: enable access to content tree, see below (Content variable)
        //cfg.setSharedVariable("Tree", new StructrTemplateNodeModel(this));

        try {

            AbstractNode callingNode = null;

            if (getTemplate(user) != null) {

                callingNode = template.getCallingNode();

                Map root = new HashMap();
                root.put("Template", this);

                if (callingNode != null) {
                    root.put(callingNode.getType(), callingNode);
                }

                HttpServletRequest request = StructrContext.getRequest();
                if (request != null) {
                    //root.put("Request", new freemarker.template.SimpleHash(request.getParameterMap().));
                    root.put("Request", new freemarker.ext.servlet.HttpRequestParametersHashModel(request));

                    // if search string is given, put search results into freemarker model
                    String searchString = request.getParameter("search");
                    if (searchString != null && !(searchString.isEmpty())) {
                        Command search = Services.command(SearchNodeCommand.class);
                        List<AbstractNode> result = (List<AbstractNode>) search.execute(
                                null, // user => null means super user
                                null, // top node => null means search all
                                false, // include hidden
                                true, // public only
                                Search.orName(searchString)); // search in name
                        root.put("SearchResults", result);
                    }
                }

                if (user != null) {
                    root.put("User", user);
                }

                // Add a generic helper
                root.put("Helper", new TemplateHelper());

		// Add error and ok message if present
		HttpSession session = StructrContext.getSession();
		if(session != null)
		{
			if(session.getAttribute("errorMessage") != null)
			{
				root.put("ErrorMessage", session.getAttribute("errorMessage"));
			}

			if(session.getAttribute("errorMessage") != null)
			{
				root.put("OkMessage", session.getAttribute("okMessage"));
			}
		}

                // add geo info if available
                // TODO: add geo node information


                //root.put("Content", new StructrTemplateNodeModel(this, startNode, editUrl, editNodeId, user));
                //root.put("ContextPath", callingNode.getNodePath(startNode));

                freemarker.template.Template t = new freemarker.template.Template(template.getName(), new StringReader(templateString), cfg);
                t.process(root, out);

            } else {

                // if no template is given, just copy the input
                out.write(templateString);
                out.flush();

            }

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error: {0}", t.getMessage());
        }

    }
}
