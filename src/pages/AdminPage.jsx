import { useState, useEffect } from 'react';
import { adminApi } from '../services/api';
import AppShell from '../components/AppShell';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';
import { format } from 'date-fns';

export default function AdminPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await adminApi.getUsers();
      setUsers(data);
    } catch { toast.error('Failed to load users'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const toggleRole = async (user) => {
    if (user.id === currentUser?.id) {
      toast.error("You can't change your own role");
      return;
    }
    const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    if (!confirm(`Change ${user.name}'s role to ${newRole}?`)) return;
    setUpdatingId(user.id);
    try {
      await adminApi.updateRole(user.id, newRole);
      toast.success('Role updated');
      load();
    } catch { toast.error('Failed to update role'); }
    finally { setUpdatingId(null); }
  };

  const deleteUser = async (user) => {
    if (user.id === currentUser?.id) {
      toast.error("You can't delete yourself");
      return;
    }
    if (!confirm(`Delete user "${user.name}"? This will also delete all their tasks.`)) return;
    try {
      await adminApi.deleteUser(user.id);
      toast.success('User deleted');
      load();
    } catch { toast.error('Failed to delete user'); }
  };

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h2 className="page-title">User Management</h2>
          <p className="page-subtitle">Manage user accounts and roles</p>
        </div>
        <div className="stat-card" style={{ padding: '12px 20px', minWidth: 120 }}>
          <div className="stat-label">Total Users</div>
          <div className="stat-value">{users.length}</div>
        </div>
      </div>

      <div className="page-body">
        <div className="card">
          {loading ? (
            <div className="loading-page"><span className="spinner" /></div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Joined</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(u => (
                    <tr key={u.id}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div className="avatar" style={{ width: 28, height: 28, fontSize: '0.7rem' }}>
                            {u.name.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase()}
                          </div>
                          <span style={{ fontWeight: 600 }}>
                            {u.name}
                            {u.id === currentUser?.id && (
                              <span style={{ fontSize: '0.75rem', color: 'var(--accent)', marginLeft: 6 }}>you</span>
                            )}
                          </span>
                        </div>
                      </td>
                      <td style={{ color: 'var(--text2)' }}>{u.email}</td>
                      <td>
                        <span className={`badge ${u.role === 'ADMIN' ? 'badge-IN_PROGRESS' : 'badge-TODO'}`}>
                          {u.role}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text2)', fontSize: '0.85rem' }}>
                        {u.createdAt ? format(new Date(u.createdAt), 'MMM d, yyyy') : '—'}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                          <button
                            className="btn btn-ghost btn-sm"
                            disabled={updatingId === u.id || u.id === currentUser?.id}
                            onClick={() => toggleRole(u)}>
                            {updatingId === u.id
                              ? <span className="spinner" style={{ width: 14, height: 14 }} />
                              : u.role === 'ADMIN' ? 'Make User' : 'Make Admin'}
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            disabled={u.id === currentUser?.id}
                            onClick={() => deleteUser(u)}>
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}
