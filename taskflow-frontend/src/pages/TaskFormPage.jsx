import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { taskApi } from '../services/api';
import AppShell from '../components/AppShell';
import toast from 'react-hot-toast';

const STATUS_OPTIONS = ['TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export default function TaskFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    title: '', description: '', status: 'TODO',
    priority: 'MEDIUM', dueDate: '',
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [fetchLoading, setFetchLoading] = useState(isEdit);

  useEffect(() => {
    if (!isEdit) return;
    taskApi.getOne(id)
      .then(({ data }) => {
        setForm({
          title: data.title || '',
          description: data.description || '',
          status: data.status || 'TODO',
          priority: data.priority || 'MEDIUM',
          dueDate: data.dueDate || '',
        });
      })
      .catch(() => { toast.error('Task not found'); navigate('/dashboard'); })
      .finally(() => setFetchLoading(false));
  }, [id, isEdit, navigate]);

  const handle = (e) => {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }));
    setErrors(err => ({ ...err, [e.target.name]: '' }));
  };

  const validate = () => {
    const errs = {};
    if (!form.title.trim()) errs.title = 'Title is required';
    if (form.title.length > 200) errs.title = 'Max 200 characters';
    return errs;
  };

  const submit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    setLoading(true);
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim() || null,
        status: form.status,
        priority: form.priority,
        dueDate: form.dueDate || null,
      };
      if (isEdit) {
        await taskApi.update(id, payload);
        toast.success('Task updated!');
      } else {
        await taskApi.create(payload);
        toast.success('Task created!');
      }
      navigate('/dashboard');
    } catch (err) {
      const fieldErrors = err.response?.data?.fieldErrors;
      if (fieldErrors) setErrors(fieldErrors);
      else toast.error(err.response?.data?.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  if (fetchLoading) return (
    <AppShell>
      <div className="loading-page"><span className="spinner" /></div>
    </AppShell>
  );

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <h2 className="page-title">{isEdit ? 'Edit Task' : 'New Task'}</h2>
          <p className="page-subtitle">{isEdit ? 'Update task details' : 'Create a new task'}</p>
        </div>
      </div>

      <div className="page-body">
        <div className="card" style={{ maxWidth: 640 }}>
          <form onSubmit={submit}>
            <div className="card-body">
              <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                <div className="form-group">
                  <label className="form-label">Title *</label>
                  <input className={`form-input ${errors.title ? 'error' : ''}`}
                    name="title" value={form.title} onChange={handle}
                    placeholder="What needs to be done?" autoFocus />
                  {errors.title && <span className="form-error">{errors.title}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label">Description</label>
                  <textarea className="form-textarea"
                    name="description" value={form.description} onChange={handle}
                    placeholder="Add details, context, or notes..." />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Status</label>
                    <select className="form-select" name="status" value={form.status} onChange={handle}>
                      {STATUS_OPTIONS.map(s => (
                        <option key={s} value={s}>{s.replace('_', ' ')}</option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Priority</label>
                    <select className="form-select" name="priority" value={form.priority} onChange={handle}>
                      {PRIORITY_OPTIONS.map(p => (
                        <option key={p} value={p}>{p}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Due Date</label>
                  <input className="form-input" type="date" name="dueDate"
                    value={form.dueDate} onChange={handle} />
                </div>
              </div>
            </div>

            <div style={{ padding: '16px 24px', borderTop: '1px solid var(--border)' }}>
              <div className="form-actions">
                <button type="button" className="btn btn-ghost" onClick={() => navigate(-1)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading
                    ? <span className="spinner" style={{ width: 16, height: 16 }} />
                    : isEdit ? 'Save Changes' : 'Create Task'
                  }
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </AppShell>
  );
}
