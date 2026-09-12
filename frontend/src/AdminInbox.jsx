import React, { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export default function AdminInbox() {
  const [conversations, setConversations] = useState([
    { id: 1, name: "Rahul Sharma", phone: "+91 9876543210", lastMessage: "Can I get a discount?", status: "OPEN" }
  ]);
  const [activeConv, setActiveConv] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const stompClient = useRef(null);
  const chatEndRef = useRef(null);

  useEffect(() => {
    // In a real app, fetch conversations and messages for the active conversation from REST API first.
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws-sareekart'),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('Connected to STOMP');
        client.subscribe('/topic/admin/inbox', (message) => {
          const newMsg = JSON.parse(message.body);
          setMessages(prev => [...prev, newMsg]);
          // Also update the conversation list snippet
        });
      }
    });
    client.activate();
    stompClient.current = client;

    return () => client.deactivate();
  }, []);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = () => {
    if (!input.trim() || !activeConv) return;
    
    // Publish to backend
    stompClient.current.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify({ conversationId: activeConv.id, content: input })
    });
    
    setInput('');
  };

  return (
    <div style={{ display: "flex", height: "calc(100vh - 60px)", background: "#faf9f7" }}>
      {/* ── CONVERSATION LIST ── */}
      <div style={{ width: 320, background: "#fff", borderRight: "1px solid #eee", display: "flex", flexDirection: "column" }}>
        <div style={{ padding: 16, borderBottom: "1px solid #eee" }}>
          <h2 style={{ margin: 0, fontSize: 18, color: "#1a0a00" }}>Inbox</h2>
        </div>
        <div style={{ flex: 1, overflowY: "auto" }}>
          {conversations.map(conv => (
            <div 
              key={conv.id} 
              onClick={() => setActiveConv(conv)}
              style={{ 
                padding: "16px", 
                borderBottom: "1px solid #f9f9f9", 
                cursor: "pointer",
                background: activeConv?.id === conv.id ? "#fdfbf7" : "#fff",
                borderLeft: activeConv?.id === conv.id ? "3px solid #d4a855" : "3px solid transparent"
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                <span style={{ fontWeight: 600, fontSize: 14 }}>{conv.name}</span>
                <span style={{ fontSize: 11, color: "#888" }}>Just now</span>
              </div>
              <div style={{ fontSize: 13, color: "#666", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                {conv.lastMessage}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── CHAT AREA ── */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
        {activeConv ? (
          <>
            <div style={{ padding: "16px 24px", background: "#fff", borderBottom: "1px solid #eee", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <div>
                <h3 style={{ margin: 0, fontSize: 16 }}>{activeConv.name}</h3>
                <div style={{ fontSize: 12, color: "#2e7d32", display: "flex", alignItems: "center", gap: 4 }}>
                  <span style={{ width: 8, height: 8, background: "#2e7d32", borderRadius: "50%", display: "inline-block" }}></span>
                  Online on WhatsApp
                </div>
              </div>
              <button style={{ background: "#f5f5f5", border: "none", padding: "6px 12px", borderRadius: 4, cursor: "pointer", fontSize: 12, fontWeight: 600 }}>Close Ticket</button>
            </div>
            
            <div style={{ flex: 1, padding: 24, overflowY: "auto", display: "flex", flexDirection: "column", gap: 16 }}>
              {messages.map((msg, i) => {
                const isAdmin = msg.senderType === 'ADMIN';
                return (
                  <div key={i} style={{ alignSelf: isAdmin ? "flex-end" : "flex-start", maxWidth: "60%" }}>
                    <div style={{ 
                      background: isAdmin ? "#800020" : "#fff", 
                      color: isAdmin ? "#fff" : "#1a0a00", 
                      padding: "10px 16px", 
                      borderRadius: 12,
                      borderBottomRightRadius: isAdmin ? 4 : 12,
                      borderBottomLeftRadius: isAdmin ? 12 : 4,
                      boxShadow: "0 2px 8px rgba(0,0,0,0.05)"
                    }}>
                      {msg.content}
                    </div>
                    <div style={{ fontSize: 10, color: "#aaa", marginTop: 4, textAlign: isAdmin ? "right" : "left" }}>
                      {new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                    </div>
                  </div>
                );
              })}
              <div ref={chatEndRef} />
            </div>

            <div style={{ padding: 16, background: "#fff", borderTop: "1px solid #eee", display: "flex", gap: 12 }}>
              <input 
                type="text" 
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && sendMessage()}
                placeholder="Type a message to reply on WhatsApp..." 
                style={{ flex: 1, padding: "12px 16px", border: "1px solid #ddd", borderRadius: 24, fontSize: 14, outline: "none" }}
              />
              <button 
                onClick={sendMessage}
                style={{ background: "#800020", color: "#fff", border: "none", width: 44, height: 44, borderRadius: "50%", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}
              >
                ➤
              </button>
            </div>
          </>
        ) : (
          <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", color: "#888", flexDirection: "column", gap: 12 }}>
            <div style={{ fontSize: 48 }}>💬</div>
            Select a conversation to start messaging
          </div>
        )}
      </div>

      {/* ── CUSTOMER PROFILE PANE ── */}
      {activeConv && (
        <div style={{ width: 280, background: "#fff", borderLeft: "1px solid #eee", padding: 24 }}>
          <div style={{ width: 80, height: 80, background: "#eee", borderRadius: "50%", margin: "0 auto 16px", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 24 }}>👤</div>
          <h3 style={{ margin: "0 0 4px", textAlign: "center", fontSize: 16 }}>{activeConv.name}</h3>
          <p style={{ margin: "0 0 24px", textAlign: "center", fontSize: 13, color: "#666" }}>{activeConv.phone}</p>
          
          <div style={{ marginBottom: 20 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: "#888", textTransform: "uppercase", marginBottom: 8 }}>Tags</div>
            <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
              <span style={{ background: "#e8f5e9", color: "#2e7d32", padding: "4px 8px", borderRadius: 4, fontSize: 11, fontWeight: 600 }}>VIP Customer</span>
              <span style={{ background: "#fff3e0", color: "#ef6c00", padding: "4px 8px", borderRadius: 4, fontSize: 11, fontWeight: 600 }}>Discount Seeker</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
