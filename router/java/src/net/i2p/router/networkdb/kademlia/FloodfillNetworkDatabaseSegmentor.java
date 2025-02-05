package net.i2p.router.networkdb.kademlia;

import net.i2p.data.Hash;
import net.i2p.router.NetworkDatabaseFacade;
import net.i2p.router.RouterContext;
import net.i2p.util.Log;

/**
 * FloodfillNetworkDatabaseSegmentor
 * 
 * Default implementation of the SegmentedNetworkDatabaseFacade.
 * 
 * This is a datastructure which manages (3+Clients) "sub-netDbs" on behalf of an
 * I2P router, each representing it's own view of the network. Normally, these sub-netDb's
 * are identified by the hash of the primary session belonging to the client who "owns"
 * a particular sub-netDb.
 * 
 * There is one "Special" netDb which has a non-hash name. This is used for the operation of
 * router itself and not clients, in particular when acting as a floodfill:
 * 
 *  - Main NetDB: This is the netDb we use if or when we become a floodfill, and for
 *  direct interaction with other routers on the network, such as when we are communicating
 *  with a floodfill.
 * 
 * It is possible that it may be advantageous some day to have other netDb's for specific use
 * cases, but that is not the purpose of this class at this time.
 * 
 * And there are an unlimited number of "Client" netDbs. These sub-netDbs are
 * intended to contain only the information required to operate them, and as such
 * most of them are very small, containing only a few LeaseSets belonging to clients.
 * Each one corresponds to a Destination which can recieve information from the
 * netDb, and can be indexed either by it's hash or by it's base32 address. This index
 * is known as the 'dbid' or database id.
 * 
 * Users of this class should strive to always access their sub-netDbs via the
 * explicit DBID of the destination recipient, or using the DBID of the special
 * netDb when it's appropriate to route the netDb entry to one of the special tables.
 * 
 * @author idk
 * @since 0.9.60
 */
public class FloodfillNetworkDatabaseSegmentor extends SegmentedNetworkDatabaseFacade {
    private final Log _log;
    private final RouterContext _context;
    //private static final String PROP_NETDB_ISOLATION = "router.netdb.isolation";
    public static final Hash MAIN_DBID = null;
    private final FloodfillNetworkDatabaseFacade _mainDbid;

    /**
     * Construct a new FloodfillNetworkDatabaseSegmentor with the given
     * RouterContext, containing a default, main netDb
     * and which is prepared to add client netDbs.
     * 
     * @since 0.9.60
     */
    public FloodfillNetworkDatabaseSegmentor(RouterContext context) {
        _log = context.logManager().getLog(getClass());
        _context = context;
        _mainDbid = new FloodfillNetworkDatabaseFacade(_context, MAIN_DBID);
    }

    /* Commented out prior to 2.4.0 release, might be worth resurrecting at some point
    public boolean useSubDbs() {
        return _context.getProperty(PROP_NETDB_ISOLATION, true);
    }*/

    /**
     * Retrieves the FloodfillNetworkDatabaseFacade object for the specified ID.
     * If the ID is null, the main database is returned.
     *
     * @param  id  the ID of the FloodfillNetworkDatabaseFacade object to retrieve
     * @return     the FloodfillNetworkDatabaseFacade object corresponding to the ID or null if it does not exist.
     */
    private NetworkDatabaseFacade getSubNetDB(Hash id) {
        return _context.clientManager().getClientFloodfillNetworkDatabaseFacade(id);
    }


    /**
     * If we are floodfill, turn it off and tell everybody for the _mainDbid
     * 
     * @since 0.9.60
     * 
     */
    public synchronized void shutdown() {
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("shutdown called from FNDS, shutting down main and multihome db");
        _mainDbid.shutdown();
    }

    /**
     * Start up the _mainDbid
     * 
     * @since 0.9.60
     * 
     */
    public synchronized void startup() {
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("startup called from FNDS, starting up main and multihome db");
        _mainDbid.startup();
    }

    /**
     * get the main netDb, which is the one we will use if we are a floodfill
     * 
     * @since 0.9.60
     * @return may be null
     */
    @Override
    public NetworkDatabaseFacade mainNetDB() {
        return _mainDbid;
    }

    /**
     * get the client netDb for the given id
     * Will return the "main" netDb if
     * the dbid is null.
     * 
     * @since 0.9.60
     * @return may be null if the client netDb does not exist
     */
    @Override
    public NetworkDatabaseFacade clientNetDB(Hash id) {
        if (_log.shouldDebug())
            _log.debug("looked up clientNetDB: " + id);
        if (id != null){
            NetworkDatabaseFacade fndf = getSubNetDB(id);
            if (fndf != null)
                return fndf;
        }
        return mainNetDB();
    }
}
