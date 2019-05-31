# The maximum socket receive buffer size which may be set by using the SO_RCVBUF socket option:
sysctl -w net.core.rmem_max=26214400

# The default setting in bytes of the socket receive buffer:
sysctl -w net.core.rmem_default=26214400

# The kernel parameter “netdev_max_backlog” is the maximum size of the receive queue. 
# The received frames will be stored in this queue after taking them from the ring buffer on the NIC.
# Use high value for high speed cards to prevent loosing packets.
sysctl -w net.core.netdev_max_backlog=250000

