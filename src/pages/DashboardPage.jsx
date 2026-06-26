import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { taskApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import AppShell from '../components/AppShell';
import toast from 'react-hot-toast';
import { format } from 'date-fns';

const STATUS_OPTIONS = ['', 'TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const PRIORITY_OPTIONS = ['', 'LOW', 'MEDIUM', 'HIGH', 'URGENT'];

function StatusBadge({ status }) {
  const label = { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done', CANCELLED: 'Cancelled' }[status] || status;
  return <span className={`badge badge-${status}`}>{label}</span>;
}

function TaskCard({ task, onEdit, onDelete }) {
  return (
    <div className="task-card" onClick={() => onEdit(task)}>
      <div className={`task-priority-dot priority-${task.priority}`} title={task.priority} />
      <div className="task-info">
        <div className="task-title">{task.title}</div>
        <div className="task-meta">
          <StatusBadge status={task.status} />
          {task.dueDate && <span>Due {format(new Date(task.dueDate), 'MMM d, yyyy')}</span>}
          {task.userName && <span>· {task.userName}</span>}
        </div>
      </div>
      <div className="task-actions" onClick={e => e.stopPropagation()}>
        <button className="btn btn-ghost btn-sm" onClick={() => onEdit(task)}>Edit</button>
        <button className="btn btn-danger btn-sm" onClick={() => onDelete(task)}>Delete</button>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();

  const [tasks, setTasks] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ status: '', priority: '' });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [deletingId, setDeletingId] = useState(null);

  const loadTasks = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 10 };
      if (filters.status) params.status = filters.status;
      if (filters.priority) params.priority = filters.priority;
      const { data } = await taskApi.getAll(params);
      setTasks(data.content);
      setTotalPages(data.totalPages);
    } catch (e) {
      toast.error('Failed to load tasks');
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  const loadStats = useCallback(async () => {
    try {
      const { data } = await taskApi.getStats();
      setStats(data);
    } catch {}
  }, []);

  useEffect(() => { loadTasks(); }, [loadTasks]);
  useEffect(() => { loadStats(); }, [loadStats]);

  const handleDelete = async (task) => {
    if (!confirm(`Delete "${task.title}"?`)) return;
    setDeletingId(task.id);
    try {
      await taskApi.delete(task.id);
      toast.success('Task deleted');
      loadTasks(); loadStats();
    } catch { toast.error('Delete failed'); }
    finally { setDeletingId(null); }
  };

  const handleFilterChange = (key, val) => {
    setFilters(f => ({ ...f, [key]: val }));
    setPage(0);
  };

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h2 className="page-title">
            {isAdmin ? 'All Tasks' : 'My Tasks'}
          </h2>
          <p className="page-subtitle">
            {isAdmin ? 'Managing tasks for all users' : `Welcome back, ${user?.name}`}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/tasks/new')}>
          + New Task
        </button>
      </div>

      <div className="page-body">
        {/* Stats */}
        {stats && (
          <div className="stats-grid">
            {[
              { label: 'Total', value: stats.total, color: 'var(--text)' },
              { label: 'To Do', value: stats.todo, color: 'var(--text2)' },
              { label: 'In Progress', value: stats.inProgress, color: 'var(--accent2)' },
              { label: 'Done', value: stats.done, color: 'var(--green)' },
            ].map(s => (
              <div key={s.label} className="stat-card">
                <div className="stat-label">{s.label}</div>
                <div className="stat-value" style={{ color: s.color }}>{s.value}</div>
              </div>
            ))}
          </div>
        )}

        {/* Filters */}
        <div className="filters-bar">
          <select className="filter-select" value={filters.status}
            onChange={e => handleFilterChange('status', e.target.value)}>
            {STATUS_OPTIONS.map(s => (
              <option key={s} value={s}>{s || 'All Statuses'}</option>
            ))}
          </select>
          <select className="filter-select" value={filters.priority}
            onChange={e => handleFilterChange('priority', e.target.value)}>
            {PRIORITY_OPTIONS.map(p => (
              <option key={p} value={p}>{p || 'All Priorities'}</option>
            ))}
          </select>
          {(filters.status || filters.priority) && (
            <button className="btn btn-ghost btn-sm"
              onClick={() => { setFilters({ status: '', priority: '' }); setPage(0); }}>
              Clear
            </button>
          )}
        </div>

        {/* Task list */}
        {loading ? (
          <div className="loading-page"><span className="spinner" /></div>
        ) : tasks.length === 0 ? (
          <div className="empty-state">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <h3>No tasks found</h3>
            <p>Create your first task to get started</p>
            <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => navigate('/tasks/new')}>
              + Create Task
            </button>
          </div>
        ) : (
          <>
            <div className="task-list">
              {tasks.map(task => (
                <TaskCard key={task.id} task={task}
                  onEdit={(t) => navigate(`/tasks/${t.id}/edit`)}
                  onDelete={handleDelete} />
              ))}
            </div>

            {totalPages > 1 && (
              <div className="pagination">
                <button className="page-btn" disabled={page === 0} onClick={() => setPage(p => p - 1)}>‹</button>
                {[...Array(totalPages)].map((_, i) => (
                  <button key={i} className={`page-btn ${i === page ? 'active' : ''}`} onClick={() => setPage(i)}>
                    {i + 1}
                  </button>
                ))}
                <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>›</button>
              </div>
            )}
          </>
        )}
      </div>
    </AppShell>
  );
}
